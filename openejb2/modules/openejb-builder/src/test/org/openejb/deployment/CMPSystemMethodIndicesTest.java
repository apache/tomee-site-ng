/* ====================================================================
 * Redistribution and use of this software and associated documentation
 * ("Software"), with or without modification, are permitted provided
 * that the following conditions are met:
 *
 * 1. Redistributions of source code must retain copyright
 *    statements and notices.  Redistributions must also contain a
 *    copy of this document.
 *
 * 2. Redistributions in binary form must reproduce this list of
 *    conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * 3. The name "OpenEJB" must not be used to endorse or promote
 *    products derived from this Software without prior written
 *    permission of The OpenEJB Group.  For written permission,
 *    please contact openejb-group@openejb.sf.net.
 *
 * 4. Products derived from this Software may not be called "OpenEJB"
 *    nor may "OpenEJB" appear in their names without prior written
 *    permission of The OpenEJB Group. OpenEJB is a registered
 *    trademark of The OpenEJB Group.
 *
 * 5. Due credit should be given to the OpenEJB Project
 *    (http://openejb.org/).
 *
 * THIS SOFTWARE IS PROVIDED BY THE OPENEJB GROUP AND CONTRIBUTORS
 * ``AS IS'' AND ANY EXPRESSED OR IMPLIED WARRANTIES, INCLUDING, BUT
 * NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
 * FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.  IN NO EVENT SHALL
 * THE OPENEJB GROUP OR ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT,
 * INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT,
 * STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED
 * OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of the OpenEJB Project.  For more information
 * please see <http://openejb.org/>.
 *
 * ====================================================================
 */
package org.openejb.deployment;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.ejb.EntityContext;

import junit.framework.TestCase;
import org.openejb.deployment.entity.MockCMPEJB;
import org.openejb.dispatch.InterfaceMethodSignature;
import org.openejb.dispatch.SystemMethodIndices;
import org.tranql.cache.CacheSlot;
import org.tranql.cache.CacheTable;

/**
 *
 *
 * @version $Revision$ $Date$
 *
 * */
public class CMPSystemMethodIndicesTest extends TestCase {

    public void testSystemMethodIndices() throws Exception {
        CMPContainerBuilder builder = new CMPContainerBuilder();
        builder.setClassLoader(MockCMPEJB.class.getClassLoader());
        Map vopMap = builder.buildVopMap(MockCMPEJB.class, new CacheTable("mock", new CacheSlot[0], null, null, null, null), Collections.EMPTY_MAP, null, null, null, null, null, null, new LinkedHashMap());
        InterfaceMethodSignature[] signatures = (InterfaceMethodSignature[]) vopMap.keySet().toArray(new InterfaceMethodSignature[vopMap.size()]);
        SystemMethodIndices systemMethodIndices = SystemMethodIndices.createSystemMethodIndices(signatures, "setEntityContext", new String(EntityContext.class.getName()), "unsetEntityContext");
        assertFalse(systemMethodIndices.getEjbActivateInvocation(null).getMethodIndex() == -1);
        assertFalse(systemMethodIndices.getEjbLoadInvocation(null).getMethodIndex() == -1);
        assertFalse(systemMethodIndices.getEjbPassivateInvocation(null).getMethodIndex() == -1);
        assertFalse(systemMethodIndices.getEjbStoreInvocation(null).getMethodIndex() == -1);
        assertFalse(systemMethodIndices.getSetContextInvocation(null, null).getMethodIndex() == -1);
        assertFalse(systemMethodIndices.getUnsetContextInvocation(null).getMethodIndex() == -1);
    }
}
