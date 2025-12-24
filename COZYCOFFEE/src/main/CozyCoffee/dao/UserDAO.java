package dao;

import db.DatabaseConnection;
import model.User;
import org.mindrot.jbcrypt.BCrypt; // 导入BCrypt库 用于用户注册和修改密码的加密

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class UserDAO {
    // 用户注册
    public boolean saveUser(User user) {
        Connection conn = null;
        PreparedStatement stmt = null;

        try {
            conn = DatabaseConnection.getConnection();//连接数据库，创建连接对象
            String sql = "INSERT INTO users (login_name, password_hash, email, phone_number, nickname, invitation_code, " +
                    "member_level, total_points, current_points) " +
                    "VALUES (?, ?, ?, ?, ?, ?, 'basic', 0, 0)";

            stmt = conn.prepareStatement(sql);//创建执行环境 并传入SQL语句
            stmt.setString(1, user.getLoginName());
            stmt.setString(2, user.getPasswordHash());
            stmt.setString(3, user.getEmail());
            stmt.setString(4, user.getPhoneNumber());
            stmt.setString(5, user.getNickname());
            stmt.setString(6, user.getInvitationCode());

            int rowsAffected = stmt.executeUpdate();//返回非零则操作成功
            return rowsAffected > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        } finally {
            DatabaseConnection.closeResources(conn, stmt, null);
        }
    }

    // 用户更新自己的信息
    public boolean updateUserBySelf(User user, String newPassword) {
        // 只允许用户更新昵称、邮箱、手机号码和密码
        String query = "UPDATE users SET nickname = ?, email = ?, phone_number = ?, password_hash = ? WHERE id = ?";
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(query)) {

            // 设置要更新的字段值
            statement.setString(1, user.getNickname());
            statement.setString(2, user.getEmail());
            statement.setString(3, user.getPhoneNumber());

            // 如果提供了新密码，则进行哈希处理并更新
            if (newPassword != null && !newPassword.isEmpty()) {
                String hashedPassword = BCrypt.hashpw(newPassword, BCrypt.gensalt());
                statement.setString(4, hashedPassword);
            } else {
                // 如果没有提供新密码，则保持原密码不变
                statement.setString(4, user.getPasswordHash());
            }

            // 设置WHERE条件：根据用户ID确定要更新哪个用户的信息
            statement.setInt(5, user.getId());

            int rowsUpdated = statement.executeUpdate();
            return rowsUpdated > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    //*****************************************以下为管理员********************************************/

    // 不设用户添加逻辑 因为很多字段是注册自动获取 如会员id、邀请码，以及手机号码和邮箱可以去更新

    // 删除逻辑只设计通过id删除 因为管理可以通过邮箱或手机号或登入账号或用户昵称查询到该用户 再通过id的方式删除该用户
    // 删除用户
    public boolean deleteUser(int userId) {
        String query = "DELETE FROM users WHERE id = ?";
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(query)) {

            statement.setInt(1, userId);
            int rowsDeleted = statement.executeUpdate();
            return rowsDeleted > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    // 管理员更新用户信息
    public boolean updateUserByAdmin(User user) {
        String query = "UPDATE users SET login_name = ?, password_hash = ?, email = ?, phone_number = ?, nickname = ?, " +
            "member_level = ?, total_points = ?, current_points = ? WHERE id = ?";
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(query)) {

            statement.setString(1, user.getLoginName());
            statement.setString(2, user.getPasswordHash());
            statement.setString(3, user.getEmail());
            statement.setString(4, user.getPhoneNumber());
            statement.setString(5, user.getNickname());
            statement.setString(6, user.getMemberLevel());
            statement.setInt(7, user.getTotalPoints());
            statement.setInt(8, user.getCurrentPoints());
            statement.setInt(9, user.getId());

            int rowsUpdated = statement.executeUpdate();
            return rowsUpdated > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    // 根据ID查询用户
    public User getUserById(int userId) {
        String query = "SELECT * FROM users WHERE id = ?";
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(query)) {

            statement.setInt(1, userId);
            ResultSet resultSet = statement.executeQuery();

            if (resultSet.next()) {
                return extractUserFromResultSet(resultSet);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }
    // 根据用户昵称查询用户
    public User getUserByNickname(String nickname) {
        String query = "SELECT * FROM users WHERE nickname = ?";
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(query)) {

            statement.setString(1, nickname);
            ResultSet resultSet = statement.executeQuery();

            if (resultSet.next()) { //true表示成功查询到数据库表 false反之
                return extractUserFromResultSet(resultSet);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }
    // 根据登录名查询用户
    public User getUserByLoginName(String loginName) {
        Connection connection = null;
        PreparedStatement statement = null;
        ResultSet resultSet = null;
        
        try {
            connection = DatabaseConnection.getConnection();
            String query = "SELECT * FROM users WHERE login_name = ?";
            statement = connection.prepareStatement(query);
            statement.setString(1, loginName);
            resultSet = statement.executeQuery();

            if (resultSet.next()) {
                return extractUserFromResultSet(resultSet);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            DatabaseConnection.closeResources(connection, statement, resultSet);
        }
        return null;
    }

    // 根据手机号码查询用户
    public User getUserByPhoneNumber(String phoneNumber) {
        Connection connection = null;
        PreparedStatement statement = null;
        ResultSet resultSet = null;
        
        try {
            connection = DatabaseConnection.getConnection();
            String query = "SELECT * FROM users WHERE phone_number = ?";
            statement = connection.prepareStatement(query);
            statement.setString(1, phoneNumber);
            resultSet = statement.executeQuery();

            if (resultSet.next()) {
                return extractUserFromResultSet(resultSet);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            DatabaseConnection.closeResources(connection, statement, resultSet);
        }
        return null;
    }

    // 根据邮箱查询用户
    public User getUserByEmail(String email) {
        Connection connection = null;
        PreparedStatement statement = null;
        ResultSet resultSet = null;
        
        try {
            connection = DatabaseConnection.getConnection();
            String query = "SELECT * FROM users WHERE email = ?";
            statement = connection.prepareStatement(query);
            statement.setString(1, email);
            resultSet = statement.executeQuery();

            if (resultSet.next()) {
                return extractUserFromResultSet(resultSet);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            DatabaseConnection.closeResources(connection, statement, resultSet);
        }
        return null;
    }

    // 获取所有用户
    public List<User> getAllUsers() {
        List<User> users = new ArrayList<>();
        String query = "SELECT * FROM users";
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(query);
             ResultSet resultSet = statement.executeQuery()) {

            while (resultSet.next()) {
                users.add(extractUserFromResultSet(resultSet));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return users;
    }

    // 从ResultSet中提取用户信息的方法
    private User extractUserFromResultSet(ResultSet rs) throws SQLException {
        User user = new User();
        user.setId(rs.getInt("id"));
        user.setLoginName(rs.getString("login_name"));
        user.setPasswordHash(rs.getString("password_hash"));
        user.setEmail(rs.getString("email"));
        user.setPhoneNumber(rs.getString("phone_number"));
        user.setNickname(rs.getString("nickname"));
        user.setInvitationCode(rs.getString("invitation_code"));
        user.setMemberLevel(rs.getString("member_level"));
        user.setTotalPoints(rs.getInt("total_points"));
        user.setCurrentPoints(rs.getInt("current_points"));
        user.setLevelExpireTime(rs.getTimestamp("level_expire_time"));
        user.setLastSigninDate(rs.getDate("last_signin_date"));
        user.setAvatar(rs.getString("avatar"));
        return user;
    }

    /**
     * 更新用户头像
     * @param userId 用户ID
     * @param avatarPath 头像文件路径
     * @throws SQLException 如果数据库操作失败
     */
    public void updateUserAvatar(int userId, String avatarPath) throws SQLException {
        String sql = "UPDATE users SET avatar = ? WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, avatarPath);
            stmt.setInt(2, userId);
            int result = stmt.executeUpdate();
            if (result != 1) {
                throw new SQLException("更新头像失败");
            }
        }
    }

}