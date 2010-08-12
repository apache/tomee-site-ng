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
package org.apache.openejb.core.ivm;

import java.io.Externalizable;
import java.io.IOException;
import java.io.InvalidObjectException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.io.ObjectStreamException;
import java.util.ArrayList;
import java.util.List;

public class IntraVmArtifact implements Externalizable {
    private static final List<Object> staticHandles = new ArrayList<Object>();

    private static final ThreadLocal<List<Object>> threadHandles = new ThreadLocal<List<Object>>() {
        protected List<Object> initialValue() {
            return new ArrayList<Object>();
        }
    };

    // todo why not put in message catalog?
    private static final String NO_ARTIFACT_ERROR = "The artifact this object represents could not be found.";

    private int instanceHandle;
    private boolean staticArtifact;

    public IntraVmArtifact(Object obj) {
        this(obj, false);
    }

    public IntraVmArtifact(Object obj, boolean storeStatically) {
        this.staticArtifact = storeStatically;
        List<Object> list = getHandles(storeStatically);
        instanceHandle = list.size();
        list.add(obj);
    }

    private static List<Object> getHandles(boolean staticArtifact) {
        return (staticArtifact)? staticHandles: threadHandles.get();
    }

    public IntraVmArtifact() {
    }

    public void writeExternal(ObjectOutput out) throws IOException {
        out.write(instanceHandle);
        out.writeBoolean(staticArtifact);
    }

    public void readExternal(ObjectInput in) throws IOException {
        instanceHandle = in.read();
        staticArtifact = in.readBoolean();
    }

    protected Object readResolve() throws ObjectStreamException {
        List<Object> list = getHandles(staticArtifact);
        Object artifact = list.get(instanceHandle);
        if (artifact == null) throw new InvalidObjectException(NO_ARTIFACT_ERROR + instanceHandle);
        // todo WHY?
        if (list.size() == instanceHandle + 1) {
            list.clear();
        }
        return artifact;
    }

}
