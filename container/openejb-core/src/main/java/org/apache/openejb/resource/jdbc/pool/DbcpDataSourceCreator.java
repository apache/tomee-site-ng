package org.apache.openejb.resource.jdbc.pool;

import org.apache.openejb.resource.jdbc.dbcp.BasicDataSource;
import org.apache.openejb.resource.jdbc.dbcp.DbcpDataSource;
import org.apache.xbean.recipe.ObjectRecipe;
import org.apache.xbean.recipe.Option;

import javax.sql.DataSource;
import java.util.Properties;

// just a sample showing how to implement a datasourcecreator
// this one will probably not be used since dbcp has already the integration we need
public class DbcpDataSourceCreator extends PoolDataSourceCreator {
    @Override
    public DataSource pool(final String name, final DataSource ds) {
        return new DbcpDataSource(name, ds);
    }

    @Override
    public DataSource pool(final String name, final String driver, final Properties properties) {
        final ObjectRecipe serviceRecipe = new ObjectRecipe(BasicDataSource.class.getName());
        serviceRecipe.allow(Option.CASE_INSENSITIVE_PROPERTIES);
        serviceRecipe.allow(Option.IGNORE_MISSING_PROPERTIES);
        serviceRecipe.setProperty("name", name);
        if (!properties.containsKey("JdbcDriver")) {
            properties.setProperty("driverClassName", driver);
        }
        serviceRecipe.setAllProperties(properties);

        final BasicDataSource ds = (BasicDataSource) serviceRecipe.create(); // new BasicDataSource(name);
        ds.setDriverClassName(driver);
        return ds;
    }

    @Override
    protected void doDestroy(DataSource dataSource) throws Throwable {
        ((org.apache.commons.dbcp.BasicDataSource) dataSource).close();
    }
}
