/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.openejb.core.timer;

import javax.ejb.EJBException;
import javax.ejb.ScheduleExpression;
import javax.ejb.Timer;
import javax.ejb.TimerConfig;
import javax.ejb.TimerService;
import java.io.Serializable;
import java.util.Collection;
import java.util.Date;

public class TimerServiceImpl implements TimerService {
    private final EjbTimerService ejbTimerService;
    private final Object primaryKey;

    public TimerServiceImpl(EjbTimerService ejbTimerService, Object primaryKey) {
        this.ejbTimerService = ejbTimerService;
        this.primaryKey = primaryKey;
    }

    public Timer createTimer(Date initialExpiration, long intervalDuration, Serializable info) throws IllegalArgumentException, IllegalStateException, EJBException {
        return ejbTimerService.createTimer(primaryKey, initialExpiration, intervalDuration, info);
    }

    public Timer createTimer(Date expiration, Serializable info) throws IllegalArgumentException, IllegalStateException, EJBException {
        return ejbTimerService.createTimer(primaryKey, expiration, info);
    }

    public Timer createTimer(long initialDuration, long intervalDuration, Serializable info) throws IllegalArgumentException, IllegalStateException, EJBException {
        return ejbTimerService.createTimer(primaryKey, initialDuration, intervalDuration, info);
    }

    public Timer createTimer(long duration, Serializable info) throws IllegalArgumentException, IllegalStateException, EJBException {
        return ejbTimerService.createTimer(primaryKey, duration, info);
    }

    public Collection getTimers() throws IllegalStateException, EJBException {
        return ejbTimerService.getTimers(primaryKey);
    }

	public Timer createCalendarTimer(ScheduleExpression arg0)
			throws IllegalArgumentException, IllegalStateException,
			EJBException {
		//TODO: next openejb version
		throw new UnsupportedOperationException("Method not implemented: Timer createCalendarTimer(ScheduleExpression arg0)");
	}

	public Timer createCalendarTimer(ScheduleExpression arg0, TimerConfig arg1)
			throws IllegalArgumentException, IllegalStateException,
			EJBException {
		//TODO: next openejb version
		throw new UnsupportedOperationException("Method not implemented: Timer createCalendarTimer(ScheduleExpression arg0, TimerConfig arg1)");
	}

	public Timer createIntervalTimer(long arg0, long arg1, TimerConfig arg2)
			throws IllegalArgumentException, IllegalStateException,
			EJBException {
		//TODO: next openejb version
		throw new UnsupportedOperationException("Method not implemented: Timer createIntervalTimer(long arg0, long arg1, TimerConfig arg2)");
	}

	public Timer createIntervalTimer(Date arg0, long arg1, TimerConfig arg2)
			throws IllegalArgumentException, IllegalStateException,
			EJBException {
		//TODO: next openejb version
		throw new UnsupportedOperationException("Method not implemented: Timer createIntervalTimer(Date arg0, long arg1, TimerConfig arg2)");
	}

	public Timer createSingleActionTimer(long arg0, TimerConfig arg1)
			throws IllegalArgumentException, IllegalStateException,
			EJBException {
		//TODO: next openejb version
		throw new UnsupportedOperationException("Method not implemented: Timer createSingleActionTimer(long arg0, TimerConfig arg1)");
	}

	public Timer createSingleActionTimer(Date arg0, TimerConfig arg1)
			throws IllegalArgumentException, IllegalStateException,
			EJBException {
		//TODO: next openejb version
		throw new UnsupportedOperationException("Method not implemented: Timer createSingleActionTimer(Date arg0, TimerConfig arg1)");
	}
}
