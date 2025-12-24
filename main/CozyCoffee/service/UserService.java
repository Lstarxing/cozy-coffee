package service;

import dao.UserDAO;
import model.User;
import org.mindrot.jbcrypt.BCrypt;
import service.LogService;
import java.util.List;
import java.util.Random;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class UserService {
    private final UserDAO userDAO;
    private LogService logService;
    private Map<String, VerifyCodeInfo> verifyCodeMap = new ConcurrentHashMap<>();
    //ConcurrentHashMap是Java并发包（java.util.concurrent）中的一个类，它设计用于在多线程环境中高效地执行并发访问
    public UserService() {
        this.userDAO = new UserDAO();
        this.logService = new LogService();
    }

    //*****************************************用户功能********************************************/

    // 用户注册
    public boolean registerUser(User user, String password) {
        try {
            // 检查登录名是否已存在
            if (userDAO.getUserByLoginName(user.getLoginName()) != null) {
                return false;
            }
            
            // 对密码进行加密处理
            String hashedPassword = hashPassword(password);
            user.setPasswordHash(hashedPassword);
            
            // 调用 DAO 层保存用户
            return userDAO.saveUser(user);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private String hashPassword(String password) {
        return BCrypt.hashpw(password, BCrypt.gensalt(12));
    }

    // 用户更新个人信息
    public boolean updateUserBySelf(User user, String newPassword) {
        // 验证用户是否存在
        User existingUser = userDAO.getUserById(user.getId());
        if (existingUser == null) {
            return false;
        }
        return userDAO.updateUserBySelf(user, newPassword);
    }

    // 找回密码（通过手机号码）
    public boolean recoverPasswordByPhone(String phoneNumber, String newPassword) {
        User user = userDAO.getUserByPhoneNumber(phoneNumber);
        if (user != null) {
            user.setPasswordHash(newPassword);
            return userDAO.updateUserBySelf(user, newPassword);
        }
        return false;
    }

    // 找回密码（通过邮箱）
    public boolean recoverPasswordByEmail(String email, String newPassword) {
        User user = userDAO.getUserByEmail(email);
        if (user != null) {
            user.setPasswordHash(newPassword);
            return userDAO.updateUserBySelf(user, newPassword);
        }
        return false;
    }

    // 生成随机昵称
    public String generateRandomNickname() {
        String prefix = "用户";
        Random random = new Random();
        return prefix + random.nextInt(100000);
    }

    // 生成邀请码
    public String generateInvitationCode() {
        Random random = new Random();
        StringBuilder code = new StringBuilder();
        String characters = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        for (int i = 0; i < 8; i++) {
            code.append(characters.charAt(random.nextInt(characters.length())));
        }
        return code.toString();
    }

    //*****************************************管理员功能********************************************/

    // 管理员更新用户信息
    public boolean updateUserByAdmin(User user) {
        // 验证用户是否存在
        User existingUser = userDAO.getUserById(user.getId());
        if (existingUser == null) {
            return false;
        }
        return userDAO.updateUserByAdmin(user);
    }

    // 管理员删除用户
    public boolean deleteUser(int userId) {
        return userDAO.deleteUser(userId);
    }

    // 获取所有用户
    public List<User> getAllUsers() {
        return userDAO.getAllUsers();
    }

    //*****************************************查询功能********************************************/

    // 根据ID查询用户
    public User getUserById(int userId) {
        return userDAO.getUserById(userId);
    }

    // 根据昵称查询用户
    public User getUserByNickname(String nickname) {
        return userDAO.getUserByNickname(nickname);
    }

    // 根据登录名查询用户
    public User getUserByLoginName(String loginName) {
        return userDAO.getUserByLoginName(loginName);
    }

    // 根据手机号码查询用户
    public User getUserByPhoneNumber(String phoneNumber) {
        return userDAO.getUserByPhoneNumber(phoneNumber);
    }

    // 根据邮箱查询用户
    public User getUserByEmail(String email) {
        return userDAO.getUserByEmail(email);
    }

    //*****************************************验证功能********************************************/

    // 验证登录名是否可用
    public boolean isLoginNameAvailable(String loginName) {
        return userDAO.getUserByLoginName(loginName) == null;
    }

    // 验证手机号码是否已注册
    public boolean isPhoneNumberRegistered(String phoneNumber) {
        return userDAO.getUserByPhoneNumber(phoneNumber) != null;
    }

    // 验证邮箱是否已注册
    public boolean isEmailRegistered(String email) {
        return userDAO.getUserByEmail(email) != null;
    }

    // 验证用户密码
    public boolean validatePassword(String loginName, String password) {
        User user = userDAO.getUserByLoginName(loginName);
        if (user != null) {
            return BCrypt.checkpw(password, user.getPasswordHash());
        }
        return false;
    }



    // 验证码信息类
    private static class VerifyCodeInfo {
        String code;
        long expireTime;
        // 构造函数包含验证码和过期时间
        VerifyCodeInfo(String code) {
            this.code = code;
            this.expireTime = System.currentTimeMillis() + 60 * 1000; // 1分钟有效期
        }

        boolean isValid() {
            return System.currentTimeMillis() < expireTime;
        }
    }

    // 发送手机验证码
    public boolean sendPhoneVerifyCode(String phoneNumber) {
        try {
            String code = generateVerifyCode();
            verifyCodeMap.put(phoneNumber, new VerifyCodeInfo(code));
            // TODO: 实际发送短信的逻辑
            System.out.println("模拟发送短信验证码到 " + phoneNumber + ": " + code);
            logService.log("INFO", "发送验证码", "手机号: " + phoneNumber);
            return true;
        } catch (Exception e) {
            logService.log("ERROR", "发送验证码失败", "手机号: " + phoneNumber + ", 错误: " + e.getMessage());
            return false;
        }
    }

    // 发送邮箱验证码
    public boolean sendEmailVerifyCode(String email) {
        try {
            String code = generateVerifyCode();
            verifyCodeMap.put(email, new VerifyCodeInfo(code));
            // TODO: 实际发送邮件的逻辑
            System.out.println("模拟发送邮件验证码到 " + email + ": " + code);
            logService.log("INFO", "发送验证码", "邮箱: " + email);
            return true;
        } catch (Exception e) {
            logService.log("ERROR", "发送验证码失败", "邮箱: " + email + ", 错误: " + e.getMessage());
            return false;
        }
    }

    // 验证验证码
    public boolean verifyCode(String target, String code) {// target可以是手机号或邮箱 code为表单传入的验证码
        VerifyCodeInfo info = verifyCodeMap.get(target);
        if (info != null && info.isValid() && info.code.equals(code)) {
            verifyCodeMap.remove(target); // 验证成功后删除验证码
            return true;
        }
        return false;
    }

    // 生成6位随机验证码
    private String generateVerifyCode() {
        Random random = new Random();
        return String.format("%06d", random.nextInt(1000000));
    }

    /**
     * 更新用户头像
     * @param userId 用户ID
     * @param avatarPath 头像文件路径
     * @throws Exception 如果更新失败
     */
    public void updateUserAvatar(int userId, String avatarPath) throws Exception {
        try {
            userDAO.updateUserAvatar(userId, avatarPath);
        } catch (Exception e) {
            throw new Exception("更新头像失败: " + e.getMessage());
        }
    }
}