use super::*;
use crate::auth::auth_rpc;
use crate::{PasskeyCredentialDto, PasskeyDto, PasskeyLoginOptionsDto};
use tellers_mtproto::latest::api::*;

pub fn passkeys_enabled(handle: u64) -> Result<bool, MtprotoError> {
    with_client_mut(handle, |state| {
        call_with_migrate(state, |state| {
            let config: HelpAppConfig = crate::api_invoke::invoke_api(
                &mut state.snapshot,
                state.api_id,
                HelpGetAppConfigRequest { hash: 0 },
            )?;
            let HelpAppConfig::HelpAppConfig(config) = config else {
                return Ok(false);
            };
            let JsonValue::JsonObject(object) = *config.config else {
                return Ok(false);
            };
            let Vector::Vector(entries) = *object.value;
            Ok(entries.field_1.iter().any(|entry| {
            let JsonObjectValue::JsonObjectValue(entry) = entry.as_ref();
            entry.key == "settings_display_passkeys" && matches!(entry.value.as_ref(), JsonValue::JsonBool(value) if matches!(value.value.as_ref(), Bool::BoolTrue(_)))
        }))
        })
    })
}

pub fn init_passkey_login(handle: u64) -> Result<PasskeyLoginOptionsDto, MtprotoError> {
    with_client_mut(handle, |state| {
        call_with_migrate(state, |state| {
            let AuthPasskeyLoginOptions::AuthPasskeyLoginOptions(options) =
                crate::api_invoke::invoke_api(
                    &mut state.snapshot,
                    state.api_id,
                    AuthInitPasskeyLoginRequest {
                        api_id: state.api_id,
                        api_hash: state.api_hash.clone(),
                    },
                )?;
            let DataJson::DataJson(json) = *options.options;
            let snapshot = state.permanent.as_ref().unwrap_or(&state.snapshot);
            let key = snapshot
                .auth_key
                .as_deref()
                .ok_or_else(|| MtprotoError::Message("missing passkey login key".into()))?;
            Ok(PasskeyLoginOptionsDto {
                options: json.data,
                dc_id: snapshot.dc_id,
                auth_key_id: tellers_mtproto_crypto::auth_key_id(key),
            })
        })
    })
}

pub fn finish_passkey_login(
    handle: u64,
    credential: PasskeyCredentialDto,
    dc_id: i32,
    auth_key_id: i64,
) -> Result<crate::AuthSignedIn, MtprotoError> {
    let (destination, _) = crate::passkeys::login_identity(&credential.user_handle)?;
    let credential = crate::passkeys::credential(credential, false)?;
    let result = with_client_mut(handle, |state| {
        reject_if_dead(state)?;
        if state.user_id.is_some() || state.snapshot.dc_id != dc_id || !(1..=5).contains(&dc_id) {
            return Err(MtprotoError::Message("stale passkey login".into()));
        }
        let key = state
            .permanent
            .as_ref()
            .unwrap_or(&state.snapshot)
            .auth_key
            .as_deref()
            .ok_or_else(|| MtprotoError::Message("missing passkey login key".into()))?;
        if tellers_mtproto_crypto::auth_key_id(key) != auth_key_id {
            return Err(MtprotoError::Message("stale passkey login".into()));
        }
        let migrated = destination != dc_id;
        if migrated {
            ensure_ready_after_migrate(
                state,
                MtprotoError::Message(format!("MIGRATE_{destination}")),
            )?;
        }
        let response = crate::api_invoke::invoke_api(
            &mut state.snapshot,
            state.api_id,
            AuthFinishPasskeyLoginRequest {
                flags: if migrated { 1 } else { 0 },
                credential: Box::new(credential),
                from_dc_id: migrated.then_some(dc_id),
                from_auth_key_id: migrated.then_some(auth_key_id),
            },
        );
        match response {
            Ok(authorization) => {
                let signed =
                    auth_rpc::authorization_to_signed(authorization, state.snapshot.dc_id)?;
                complete_sign_in(state, signed.user_id)?;
                Ok(signed)
            }
            Err(MtprotoError::PasswordRequired) => {
                persist(state)?;
                Err(MtprotoError::PasswordRequired)
            }
            Err(error) => Err(error),
        }
    })?;
    drop_updates_transport(handle);
    Ok(result)
}

pub fn init_passkey_registration(handle: u64) -> Result<String, MtprotoError> {
    with_client_mut(handle, |state| {
        call_with_migrate(state, |state| {
            let AccountPasskeyRegistrationOptions::AccountPasskeyRegistrationOptions(options) =
                crate::api_invoke::invoke_api(
                    &mut state.snapshot,
                    state.api_id,
                    AccountInitPasskeyRegistrationRequest {},
                )?;
            let DataJson::DataJson(json) = *options.options;
            Ok(json.data)
        })
    })
}

pub fn register_passkey(
    handle: u64,
    credential: PasskeyCredentialDto,
) -> Result<PasskeyDto, MtprotoError> {
    let credential = crate::passkeys::credential(credential, true)?;
    with_client_mut(handle, |state| {
        call_with_migrate(state, |state| {
            crate::api_invoke::invoke_api(
                &mut state.snapshot,
                state.api_id,
                AccountRegisterPasskeyRequest {
                    credential: Box::new(credential.clone()),
                },
            )
            .map(crate::passkeys::to_dto)
        })
    })
}

pub fn get_passkeys(handle: u64) -> Result<Vec<PasskeyDto>, MtprotoError> {
    with_client_mut(handle, |state| {
        call_with_migrate(state, |state| {
            let AccountPasskeys::AccountPasskeys(keys) = crate::api_invoke::invoke_api(
                &mut state.snapshot,
                state.api_id,
                AccountGetPasskeysRequest {},
            )?;
            let Vector::Vector(keys) = *keys.passkeys;
            Ok(keys
                .field_1
                .into_iter()
                .map(|key| crate::passkeys::to_dto(*key))
                .collect())
        })
    })
}

pub fn delete_passkey(handle: u64, id: String) -> Result<bool, MtprotoError> {
    with_client_mut(handle, |state| {
        call_with_migrate(state, |state| {
            let result: Bool = crate::api_invoke::invoke_api(
                &mut state.snapshot,
                state.api_id,
                AccountDeletePasskeyRequest { id: id.clone() },
            )?;
            Ok(matches!(result, Bool::BoolTrue(_)))
        })
    })
}
