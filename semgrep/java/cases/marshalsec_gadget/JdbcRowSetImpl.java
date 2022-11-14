import javax.naming.InitialContext;

public class JdbcRowSetImpl {
    public void setDataSourceName(String var1) throws SQLException {  // setter
	    if (this.getDataSourceName() != null) {
	        if (!this.getDataSourceName().equals(var1)) {
	            String var2 = this.getDataSourceName();
	            super.setDataSourceName(var1);
	            this.conn = null;
	            this.ps = null;
	            this.rs = null;
	            this.propertyChangeSupport.firePropertyChange("dataSourceName", var2, var1);
	        }
	    } else {
			String varX = test(var1);
	        super.setDataSourceName(varX);
	        this.propertyChangeSupport.firePropertyChange("dataSourceName", (Object)null, var1);
	    }
	}

	public void setAutoCommit(boolean var1) throws SQLException {  // setter
	    if (this.conn != null) {
	        this.conn.setAutoCommit(var1);
	    } else {
	        this.conn = this.connect();
	        this.conn.setAutoCommit(var1);
	    }
	}
	
	public boolean getAutoCommit() throws SQLException {  // getter
	    return this.conn.getAutoCommit();
	}

    private Connection connect() throws SQLException {
        if (this.conn != null) {
            return this.conn;
        } else if (this.getDataSourceName() != null) {
            try {
                InitialContext var1 = new InitialContext();
                DataSource var2 = (DataSource)var1.lookup(this.getDataSourceName());
                return this.getUsername() != null && !this.getUsername().equals("") ? var2.getConnection(this.getUsername(), this.getPassword()) : var2.getConnection();
            } catch (NamingException var3) {
                throw new SQLException(this.resBundle.handleGetObject("jdbcrowsetimpl.connect").toString());
            }
        } else {
            return this.getUrl() != null ? DriverManager.getConnection(this.getUrl(), this.getUsername(), this.getPassword()) : null;
        }
    }
}