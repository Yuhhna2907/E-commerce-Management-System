import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * Utility để generate BCrypt hash cho password
 * Compile và chạy file này để lấy hash
 */
public class GeneratePasswordHash {
    public static void main(String[] args) {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        
        String password = "Tittom343@";
        String hash = encoder.encode(password);
        
        System.out.println("===========================================");
        System.out.println("Password: " + password);
        System.out.println("BCrypt Hash:");
        System.out.println(hash);
        System.out.println("===========================================");
        System.out.println("\nCopy hash này vào SQL UPDATE statement:");
        System.out.println("UPDATE users SET password = '" + hash + "' WHERE email = 'luonganhhuy2004@gmail.com';");
    }
}
