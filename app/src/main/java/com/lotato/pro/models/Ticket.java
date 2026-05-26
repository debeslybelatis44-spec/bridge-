package com.lotato.pro.models;

import java.util.List;

public class Ticket {
    private String ticketId;
    private String drawId;
    private String drawName;
    private List<Bet> bets;
    private double totalAmount;
    private double winAmount;
    private boolean checked;
    private boolean paid;
    private String date;
    private String agentId;
    private String agentName;

    public Ticket() {}

    public String getTicketId() { return ticketId; }
    public void setTicketId(String ticketId) { this.ticketId = ticketId; }
    public String getDrawId() { return drawId; }
    public void setDrawId(String drawId) { this.drawId = drawId; }
    public String getDrawName() { return drawName; }
    public void setDrawName(String drawName) { this.drawName = drawName; }
    public List<Bet> getBets() { return bets; }
    public void setBets(List<Bet> bets) { this.bets = bets; }
    public double getTotalAmount() { return totalAmount; }
    public void setTotalAmount(double totalAmount) { this.totalAmount = totalAmount; }
    public double getWinAmount() { return winAmount; }
    public void setWinAmount(double winAmount) { this.winAmount = winAmount; }
    public boolean isChecked() { return checked; }
    public void setChecked(boolean checked) { this.checked = checked; }
    public boolean isPaid() { return paid; }
    public void setPaid(boolean paid) { this.paid = paid; }
    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }
    public String getAgentId() { return agentId; }
    public void setAgentId(String agentId) { this.agentId = agentId; }
    public String getAgentName() { return agentName; }
    public void setAgentName(String agentName) { this.agentName = agentName; }

    public String getStatusLabel() {
        if (!checked) return "AP TANN";
        if (winAmount > 0) return "GANYEN " + (int) winAmount + " G";
        return "PEDI";
    }
}

