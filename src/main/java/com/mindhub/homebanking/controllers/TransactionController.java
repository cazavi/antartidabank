package com.mindhub.homebanking.controllers;

import com.mindhub.homebanking.models.*;
import com.mindhub.homebanking.repositories.AccountRepository;
import com.mindhub.homebanking.repositories.ClientRepository;
import com.mindhub.homebanking.repositories.TransactionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import javax.transaction.Transactional;
import java.time.LocalDateTime;

@RestController
@RequestMapping(value = "/api")
public class TransactionController {
    @Autowired
    private AccountRepository accountRepository;
    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private ClientRepository clientRepository;

    @Transactional
    @PostMapping("/transactions")
    public ResponseEntity<Object> transfer(
            Authentication authentication,
            @RequestParam String fromAccountNumber, @RequestParam String toAccountNumber,
            @RequestParam double amount, @RequestParam String description){

        Client client = this.clientRepository.findByEmail(authentication.getName());
        Account originAccount = accountRepository.findByNumber(fromAccountNumber);
        Account destinationAccount = accountRepository.findByNumber(toAccountNumber);

        //Verificar que los parámetros no estén vacíos
        if (amount <= 0 ) {
            return new ResponseEntity<>("Ingresa un monto de transacción.", HttpStatus.FORBIDDEN);
        }

        if (description.isEmpty() || fromAccountNumber.isEmpty() || toAccountNumber.isEmpty()) {
            return new ResponseEntity<>("Falta información.", HttpStatus.FORBIDDEN);
        }

        //Verificar que los números de cuenta no sean iguales
        if (fromAccountNumber == toAccountNumber) {
            return new ResponseEntity<>("Elige una cuenta de destino diferente", HttpStatus.FORBIDDEN);
        }

        //Verificar que exista la cuenta de origen
        if (originAccount == null) {
            return new ResponseEntity<>("La cuenta de origen no existe.", HttpStatus.FORBIDDEN);
        }

        //Verificar que la cuenta de origen pertenezca al cliente autenticado
        if (!client.getAccounts().contains(originAccount)){
            return new ResponseEntity<>("El cliente no posee esta cuenta.", HttpStatus.FORBIDDEN);
        }

        //Verificar que exista la cuenta de destino
        if (destinationAccount == null) {
            return new ResponseEntity<>("La cuenta de destino no existe", HttpStatus.FORBIDDEN);
        }

        //Verificar que la cuenta de origen tenga el monto disponible.
        if (originAccount.getBalance() < amount) {
            return new ResponseEntity<>("No tiene suficientes fondos.", HttpStatus.FORBIDDEN);
        }

        transactionRepository.save(new Transaction(
                TransactionType.DEBIT,
                -amount,
                description+" "+destinationAccount.getNumber(),
                LocalDateTime.now(),
                originAccount
                )
        );

        transactionRepository.save(new Transaction(
                TransactionType.CREDIT,
                +amount,
                description+" "+originAccount.getNumber(),
                LocalDateTime.now(),
                destinationAccount)
        );

        originAccount.setBalance(originAccount.getBalance() - amount);
        destinationAccount.setBalance(destinationAccount.getBalance() + amount);

        accountRepository.save(originAccount);
        accountRepository.save(destinationAccount);

        return new ResponseEntity<>(HttpStatus.CREATED);

    }

}
