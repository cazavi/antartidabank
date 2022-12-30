package com.mindhub.homebanking.dtos;

import com.mindhub.homebanking.models.Loan;
import java.util.List;

public class LoanDTO {
    private long id;
    private List<Integer> payments;
    private String name;
    private double maxAmount;

    public LoanDTO(Loan loan) {
        this.id = loan.getId();
        this.payments = loan.getPayments();
        this.name = loan.getName();
        this.maxAmount = loan.getMaxAmount();
    }

    public long getId() {
        return id;
    }

    public List<Integer> getPayments() {
        return payments;
    }

    public void setPayments(List<Integer> payments) {
        this.payments = payments;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public double getMaxAmount() {
        return maxAmount;
    }

    public void setMaxAmount(double maxAmount) {
        this.maxAmount = maxAmount;
    }
}
