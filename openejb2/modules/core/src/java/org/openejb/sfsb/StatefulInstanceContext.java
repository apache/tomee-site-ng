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
package org.openejb.sfsb;

import javax.ejb.EnterpriseBean;
import javax.ejb.SessionBean;
import javax.ejb.SessionSynchronization;

import org.apache.geronimo.connector.outbound.connectiontracking.defaultimpl.DefaultComponentContext;

import org.openejb.EJBInstanceContext;
import org.openejb.EJBOperation;
import org.openejb.proxy.EJBProxyFactory;
import org.openejb.transaction.EJBUserTransaction;

/**
 *
 *
 * @version $Revision$ $Date$
 */
public class StatefulInstanceContext extends DefaultComponentContext implements EJBInstanceContext {
    private final Object containerId;
    private final EJBProxyFactory proxyFactory;
    private final SessionBean instance;
    private final Object id;
    private final StatefulSessionContext statefulContext;
    private boolean dead = false;

    public StatefulInstanceContext(Object containerId, EJBProxyFactory proxyFactory, SessionBean instance, Object id, EJBUserTransaction userTransaction) {
        this.containerId = containerId;
        this.proxyFactory = proxyFactory;
        this.instance = instance;
        this.id = id;
        statefulContext = new StatefulSessionContext(this, userTransaction);
    }

    public Object getContainerId() {
        return containerId;
    }

    public EJBProxyFactory getProxyFactory() {
        return proxyFactory;
    }

    public void setOperation(EJBOperation operation) {
        statefulContext.setState(operation);
    }

    public Object getId() {
        return id;
    }

    public void setId(Object id) {
        // @todo remove setId from the EJBInstanceContext interface
        throw new UnsupportedOperationException();
    }

    public EnterpriseBean getInstance() {
        return instance;
    }

    public void die() {
        dead = true;
    }

    public boolean isDead() {
        return dead;
    }

    public StatefulSessionContext getSessionContext() {
        return statefulContext;
    }

    public void afterBegin() throws Exception {
        if (instance instanceof SessionSynchronization) {
            try {
                ((SessionSynchronization) instance).afterBegin();
            } catch (Exception e) {
                dead = true;
                throw e;
            } catch (Error e) {
                dead = true;
                throw e;
            }
        }
    }

    public void beforeCommit() throws Exception {
        if (instance instanceof SessionSynchronization) {
            try {
                ((SessionSynchronization) instance).beforeCompletion();
            } catch (Exception e) {
                dead = true;
                throw e;
            } catch (Error e) {
                dead = true;
                throw e;
            }
        }
    }

    public void afterCommit(boolean committed) throws Exception {
        if (!dead) {
            // @todo fix me
//            container.getInstanceCache().putInactive(id, this);
        }
        if (instance instanceof SessionSynchronization) {
            ((SessionSynchronization) instance).afterCompletion(committed);
        }
    }
}
