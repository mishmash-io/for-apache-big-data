/*
 *    Copyright 2025 Mishmash IO UK Ltd.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package io.mishmash.stacks.oidc.sasl;

import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.security.auth.Subject;
import javax.security.auth.callback.CallbackHandler;
import javax.security.sasl.SaslClient;
import javax.security.sasl.SaslClientFactory;
import javax.security.sasl.SaslException;

import io.mishmash.stacks.oidc.login.OIDCClientPrincipal;

public class OAUTHBearerClientFactory implements SaslClientFactory {

    private static final Logger LOG =
            Logger.getLogger(OAUTHBearerClientFactory.class.getName());

    @Override
    public SaslClient createSaslClient(
            final String[] mechanisms,
            final String authorizationId,
            final String protocol,
            final String serverName,
            final Map<String, ?> props,
            final CallbackHandler cbh) throws SaslException {
        LOG.log(Level.FINER, () ->
                "Creating OAUTHBEARER SASL client -"
                    + " mechanisms: " + mechanisms
                    + " authzId: " + authorizationId
                    + " protocol: " + protocol
                    + " server name: " + serverName
                    + " props: " + props);

        Subject subject = Subject.current();

        if (subject == null) {
            throw new SaslException(
                    "Could not determine Subject for sasl client");
        }

        OIDCClientPrincipal client = subject
                            .getPrincipals(OIDCClientPrincipal.class)
                                .stream()
                                .findFirst()
                                .orElse(null);
        if (client == null) {
            throw new SaslException(
                    "Could not find an OIDC client");
        }

        // prioritize the first mechanism given
        String mechanism = mechanisms[0];
        if (OAUTHBearerProvider.MECHANISM.equals(mechanism)) {
            return new OAUTHBearerClient(client,
                authorizationId,
                serverName);
        } else if (mechanism.startsWith(
                OAUTHBearerProvider.MECHANISM + "-DH")) {
            int keyLen = Integer.valueOf(
                    mechanism.substring(
                            (OAUTHBearerProvider.MECHANISM + "-DH")
                                .length()));

            switch (keyLen) {
            case 4096:
                return new OAUTHBearerClientDH(
                        client,
                        authorizationId,
                        serverName,
                        4096);
            default:
                throw new SaslException("Unsupported key length: " + keyLen);
            }
        } else {
            throw new SaslException("Unsupported mechanism " + mechanism);
        }
    }

    @Override
    public String[] getMechanismNames(final Map<String, ?> props) {
        return new String[] {
                OAUTHBearerProvider.MECHANISM,
                OAUTHBearerProvider.MECHANISM + "-DH4096"};
    }
}
