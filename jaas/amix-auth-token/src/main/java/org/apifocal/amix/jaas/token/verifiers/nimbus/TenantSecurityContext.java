/*
 * Copyright (c) 2017-2018 apifocal LLC. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apifocal.amix.jaas.token.verifiers.nimbus;

import com.nimbusds.jose.proc.SecurityContext;

import java.util.Optional;

/**
 * A high level definition of security context which is supposed to deliver tenant information.
 */
public interface TenantSecurityContext extends SecurityContext {

    /**
     * Retrieves tenant name, if available.
     *
     * @return Tenant name.
     */
    Optional<String> getTenant();

}
