package com.fpsweeper.harvest.wallet;

import com.fpsweeper.harvest.user.UserRepository;
import com.fpsweeper.harvest.user.Users;
import com.fpsweeper.harvest.wallet.dto.LinkSolanaWalletRequest;
import com.fpsweeper.harvest.wallet.dto.SolanaWalletResponse;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/api/wallet/solana")
public class SolanaWalletController {

    @Autowired
    private SolanaWalletService walletService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SolanaWalletRepository solanaWalletRepository;

    @PostMapping("/link")
    public ResponseEntity<SolanaWalletResponse> linkWallet(
            @AuthenticationPrincipal Users user,
            @Valid @RequestBody LinkSolanaWalletRequest request) {

        String email = user.getEmail();
        SolanaWalletResponse response = walletService.linkWallet(email, request);

        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/unlink")
    public ResponseEntity<String> unlinkWallet(@AuthenticationPrincipal Users user) {
        String email = user.getEmail();
        walletService.unlinkWallet(email);

        return ResponseEntity.ok("Wallet unlinked successfully");
    }

    @PostMapping("/wallet")
    public ResponseEntity<?> getLinkedWallet(@AuthenticationPrincipal Object principal) {
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(null);
        }

        Users user = null;

        // Handle JWT authentication
        if (principal instanceof Users) {
            user = (Users) principal;
        }
        // Handle OAuth2 authentication
        else if (principal instanceof OAuth2User) {
            OAuth2User oauth2User = (OAuth2User) principal;
            String twitterId = (String) oauth2User.getAttributes().get("id");
            user = userRepository.findByTwitterId(twitterId).orElse(null);
        }

        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(null);
        }

        Optional<SolanaWallets> wallet = solanaWalletRepository.findByUserId(user.getId());
        return wallet.map(w -> ResponseEntity.ok(new SolanaWalletResponse(w)))
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/status")
    public ResponseEntity<Boolean> hasLinkedWallet(@AuthenticationPrincipal Users user) {
        String email = user.getEmail();
        boolean hasWallet = walletService.hasLinkedWallet(email);

        return ResponseEntity.ok(hasWallet);
    }

    @PostMapping("/verify")
    public ResponseEntity<Void> verifyWallet(
            @RequestParam String walletAddress,
            @AuthenticationPrincipal Users user
    ) {
        walletService.verifyWallet(user.getId(), walletAddress);
        return ResponseEntity.ok().build();
    }

}