package com.mindhub.homebanking.controllers;

import com.mindhub.homebanking.dtos.LoanApplicationDTO;
import com.mindhub.homebanking.dtos.LoanDTO;
import com.mindhub.homebanking.models.*;
import com.mindhub.homebanking.repositories.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDateTime;
import java.util.List;

import static java.util.stream.Collectors.toList;

@RestController
@RequestMapping("/api")
public class LoanController {
    @Autowired
    private ClientRepository clientRepository;
    @Autowired
    private LoanRepository loanRepository;
    @Autowired
    private AccountRepository accountRepository;
    @Autowired
    private ClientLoanRepository clientLoanRepository;
    @Autowired
    private TransactionRepository transactionRepository;

    //Listar préstamos
    @GetMapping("/loans")
    public List<LoanDTO> getLoans() {
        return loanRepository.findAll().stream().map(LoanDTO::new).collect(toList());
    }

    //Solicitar un préstamo
    @Transactional
    @PostMapping("/loans")
    public ResponseEntity<Object> newLoan(@RequestBody LoanApplicationDTO loanApplicationDTO, Authentication authentication) {
        Client client = this.clientRepository.findByEmail(authentication.getName());
        long loanId = loanApplicationDTO.getLoanId();
        double amount = loanApplicationDTO.getAmount();
        int payments = loanApplicationDTO.getPayments();
        String toAccountNumber = loanApplicationDTO.getToAccountNumber();

        //Verifica que los datos no estén vacíos
        if (loanId <= 0) {
            return new ResponseEntity<>("Missing loanId", HttpStatus.FORBIDDEN);
        }

        if (amount <= 0) {
            return new ResponseEntity<>("Missing amount", HttpStatus.FORBIDDEN);
        }

        if (payments <= 0) {
            return new ResponseEntity<>("Missing payments", HttpStatus.FORBIDDEN);
        }

        if (toAccountNumber.isEmpty()) {
            return new ResponseEntity<>("Missing account number", HttpStatus.FORBIDDEN);
        }

        //Verifica que el préstamo exista
        if (!this.loanRepository.findById(loanId).isPresent()) {
            return new ResponseEntity<>("El préstamo no existe", HttpStatus.FORBIDDEN);
        }

        Loan loan = this.loanRepository.findById(loanId).get();

        //Verifica que el monto solicitado no exceda el monto máximo
        if (amount > loan.getMaxAmount()) {
            return new ResponseEntity<>("El monto solicitado excede el monto máximo", HttpStatus.FORBIDDEN);
        }

        //Verifica que la cantidad de cuotas se encuentre entre las disponibles
        if (!loan.getPayments().contains(payments)) {
            return new ResponseEntity<>("La cantidad de cuotas es incorrecta", HttpStatus.FORBIDDEN);
        }

        //Verifica que la cuenta de destino exista
        if (this.accountRepository.findByNumber(toAccountNumber) == null) {
            return new ResponseEntity<>("La cuenta no existe", HttpStatus.FORBIDDEN);
        }

        Account cuentaDestino = this.accountRepository.findByNumber(toAccountNumber);

        //Verifica que la cuenta de origen sea del cliente autenticado
        if (client.equals(cuentaDestino.getClient()) == false) {
            return new ResponseEntity<>("La cuenta no pertenece al cliente actual", HttpStatus.FORBIDDEN);
        }

        //Se crea la solicitud del préstamo
        clientLoanRepository.save(new ClientLoan(amount + (amount * 0.2), payments, client, loan));

        //Se crea la transacción
        transactionRepository.save(new Transaction(TransactionType.CREDIT, amount, loan.getName()+" loan approved", LocalDateTime.now(), cuentaDestino));

        //Se actualiza la cuenta destino
        double newBalance = cuentaDestino.getBalance() + amount;
        cuentaDestino.setBalance(newBalance);
        accountRepository.save(cuentaDestino);

        return new ResponseEntity<>(HttpStatus.CREATED);
    }
}
