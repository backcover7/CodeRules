import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.crypto.Cipher;

import java.sql.Connection;
import java.sql.SQLException;

public class Cls extends HttpServlet
{
    private static org.apache.log4j.Logger log = Logger.getLogger(Register.class);
    String trans = "AES/CBC/PKCS5Padding";

    // cf. https://find-sec-bugs.github.io/bugs.htm#TDES_USAGE
    protected void danger(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        // ruleid:cbc-padding-oracle
        Cipher c = Cipher.getInstance(trans);
        c.init(Cipher.ENCRYPT_MODE, k, iv);
        byte[] cipherText = c.doFinal(plainText);
    }

    protected void ok(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        // ok:cbc-padding-oracle
        Cipher c = Cipher.getInstance("AES/GCM/NoPadding");
        c.init(Cipher.ENCRYPT_MODE, k, iv);
        byte[] cipherText = c.doFinal(plainText);
    }

    void setConn(Connection conn) throws SQLException {
        conn.setCatalog("EXTERNAL_CONFIG_CONTROL");
    }
}
