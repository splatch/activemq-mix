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
package org.apifocal.amix.plugins.metrics.threading;

import com.codahale.metrics.Timer;
import org.apifocal.amix.plugins.metrics.MeterContext;

import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;

public class MeteredRejectedExecutionHandler implements RejectedExecutionHandler {

    private final MeterContext meter;
    private final RejectedExecutionHandler delegate;

    public MeteredRejectedExecutionHandler(MeterContext meter, RejectedExecutionHandler delegate) {
        this.meter = meter;
        this.delegate = delegate;
    }

    @Override
    public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
        Timer executions = meter.timer("rejectedExecutions");
        executions.time(() -> delegate.rejectedExecution(r, executor));
    }

}
