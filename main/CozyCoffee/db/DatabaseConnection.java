package db;

import java.io.InputStream;
import java.sql.*;
import java.util.Properties;

/**
 * 数据库连接管理类，负责从配置文件中加载数据库连接信息并提供单例模式的数据库连接。
 */
public class DatabaseConnection {
    private static String URL;
    private static String USER;
    private static String PASSWORD;
    private static String DRIVER;

    static {
        try {
            // 加载配置文件
            Properties props = new Properties();
            InputStream input = DatabaseConnection.class.getClassLoader()
                    .getResourceAsStream("db.properties");

            if (input == null) {
                throw new RuntimeException("无法找到 db.properties 配置文件");
            }

            // 读取配置信息
            props.load(input);
            DRIVER = props.getProperty("db.driver");
            URL = props.getProperty("db.url");
            USER = props.getProperty("db.user");
            PASSWORD = props.getProperty("db.password");

            // 加载数据库驱动
            Class.forName(DRIVER);

            // 关闭输入流
            input.close();

        } catch (Exception e) {
            throw new RuntimeException("初始化数据库连接失败: " + e.getMessage());
        }
    }

    // 获取数据库连接
    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }
    // 关闭资源
    public static void closeResources(Connection conn, PreparedStatement stmt, ResultSet rs) {
        try {
            if (rs != null) {
                rs.close();
            }
            if (stmt != null) {
                stmt.close();
            }
            if (conn != null) {
                conn.close();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
