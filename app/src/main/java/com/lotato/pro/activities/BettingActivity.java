package com.lotato.pro.activities;

import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.lotato.pro.R;
import com.lotato.pro.api.ApiService;
import com.lotato.pro.models.AppSession;
import com.lotato.pro.models.Bet;
import com.lotato.pro.models.Ticket;
import com.lotato.pro.print.SunmiPrintHelper;
import com.lotato.pro.utils.GameEngine;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public class BettingActivity extends AppCompatActivity {

    private String drawId, drawName;
    private String selectedGame = "borlette";
    private List<Bet> cart = new ArrayList<>();
    private boolean[] lotto4Options = {true, true, true};
    private boolean[] lotto5Options = {true, true, true};

    private EditText etNumber, etAmount;
    private LinearLayout cartList;
    private TextView tvTotal, tvItemCount, tvDrawTitle;
    private Button btnAddBet, btnPrint;
    private View btnBorlette, btnLotto, btnSpecial;
    private LinearLayout lottoGames, specialGames;
    private View progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_betting);

        drawId = getIntent().getStringExtra("drawId");
        drawName = getIntent().getStringExtra("drawName");

        tvDrawTitle = findViewById(R.id.tvDrawTitle);
        tvDrawTitle.setText(drawName);

        etNumber = findViewById(R.id.etNumber);
        etAmount = findViewById(R.id.etAmount);
        cartList = findViewById(R.id.cartList);
        tvTotal = findViewById(R.id.tvTotal);
        tvItemCount = findViewById(R.id.tvItemCount);
        btnAddBet = findViewById(R.id.btnAddBet);
        btnPrint = findViewById(R.id.btnPrint);
        lottoGames = findViewById(R.id.lottoGames);
        specialGames = findViewById(R.id.specialGames);
        progressBar = findViewById(R.id.progressBar);

        btnBorlette = findViewById(R.id.btnBorlette);
        btnLotto = findViewById(R.id.btnLotto);
        btnSpecial = findViewById(R.id.btnSpecial);

        btnBorlette.setOnClickListener(v -> selectMainGame("borlette"));
        btnLotto.setOnClickListener(v -> toggleLottoGames());
        btnSpecial.setOnClickListener(v -> toggleSpecialGames());

        setupGameButtons();

        btnAddBet.setOnClickListener(v -> addBet());
        btnPrint.setOnClickListener(v -> processPrint());

        etAmount.setOnEditorActionListener((v, actionId, event) -> {
            addBet();
            return true;
        });

        etNumber.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                int max = GameEngine.getMaxLengthForGame(selectedGame);
                if (max > 0 && s.length() >= max) etAmount.requestFocus();
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        selectMainGame("borlette");
        renderCart();
    }

    private void setupGameButtons() {
        setupBtn(R.id.btnBorlette2, "borlette");
        setupBtn(R.id.btnLotto3, "lotto3");
        setupBtn(R.id.btnLotto4, "lotto4");
        setupBtn(R.id.btnLotto5, "lotto5");
        setupBtn(R.id.btnLotto4Auto, "auto_lotto4");
        setupBtn(R.id.btnLotto5Auto, "auto_lotto5");
        setupBtn(R.id.btnBO, "bo");
        setupBtn(R.id.btnGRAP, "grap");
        setupBtn(R.id.btnMariage, "mariage");
        setupBtn(R.id.btnMariageAuto, "auto_marriage");

        int[] nBtns = {R.id.btnN0, R.id.btnN1, R.id.btnN2, R.id.btnN3, R.id.btnN4,
                       R.id.btnN5, R.id.btnN6, R.id.btnN7, R.id.btnN8, R.id.btnN9};
        for (int i = 0; i < nBtns.length; i++) {
            final int digit = i;
            View btn = findViewById(nBtns[i]);
            if (btn != null) btn.setOnClickListener(v -> selectGame("n" + digit));
        }
    }

    private void setupBtn(int id, String game) {
        View btn = findViewById(id);
        if (btn != null) btn.setOnClickListener(v -> selectGame(game));
    }

    private void selectMainGame(String game) {
        selectedGame = game;
        lottoGames.setVisibility(View.GONE);
        specialGames.setVisibility(View.GONE);
        updateGameUI();
    }

    private void toggleLottoGames() {
        boolean showing = lottoGames.getVisibility() == View.VISIBLE;
        lottoGames.setVisibility(showing ? View.GONE : View.VISIBLE);
        specialGames.setVisibility(View.GONE);
    }

    private void toggleSpecialGames() {
        boolean showing = specialGames.getVisibility() == View.VISIBLE;
        specialGames.setVisibility(showing ? View.GONE : View.VISIBLE);
        lottoGames.setVisibility(View.GONE);
    }

    private void selectGame(String game) {
        selectedGame = game;
        updateGameUI();
        boolean isAuto = game.contains("auto") || game.equals("bo") || game.equals("grap")
                         || game.startsWith("n");
        etNumber.setEnabled(!isAuto);
        if (isAuto) {
            etNumber.setText("");
            etNumber.setHint("Auto");
        } else {
            etNumber.setEnabled(true);
            etNumber.setText("");
            switch (game) {
                case "borlette": etNumber.setHint("00"); break;
                case "lotto3": etNumber.setHint("000"); break;
                case "lotto4": etNumber.setHint("0000"); break;
                case "lotto5": etNumber.setHint("00000"); break;
                case "mariage": etNumber.setHint("0000"); break;
                default: etNumber.setHint("Nimewo"); break;
            }
            etNumber.requestFocus();
        }
    }

    private void updateGameUI() {
    }

    private void addBet() {
        String amtStr = etAmount.getText().toString().trim();
        if (amtStr.isEmpty()) { showToast("Antre montan an"); return; }
        double amt;
        try { amt = Double.parseDouble(amtStr); } catch (Exception e) {
            showToast("Montan pa valid"); return;
        }
        if (amt <= 0) { showToast("Montan pa valid"); return; }

        List<Bet> newBets = new ArrayList<>();

        switch (selectedGame) {
            case "bo":
                newBets = GameEngine.generateBOBets(amt, drawId, drawName); break;
            case "grap":
                newBets = GameEngine.generateGRAPBets(amt, drawId, drawName); break;
            case "auto_marriage":
                newBets = GameEngine.generateAutoMarriageBets(cart, amt, drawId, drawName); break;
            case "auto_lotto4":
                newBets = GameEngine.generateAutoLotto4Bets(cart, amt, drawId, drawName); break;
            case "auto_lotto5":
                newBets = GameEngine.generateAutoLotto5Bets(cart, amt, drawId, drawName); break;
            default:
                if (selectedGame.startsWith("n")) {
                    int digit = Integer.parseInt(selectedGame.substring(1));
                    newBets = GameEngine.generateNBets(digit, amt, drawId, drawName);
                } else if ("lotto4".equals(selectedGame) || "lotto5".equals(selectedGame)) {
                    String num = etNumber.getText().toString().trim();
                    if (!GameEngine.validateEntry(selectedGame, num)) {
                        showToast("Nimewo pa valid"); return;
                    }
                    boolean[] opts = "lotto4".equals(selectedGame) ? lotto4Options : lotto5Options;
                    newBets = GameEngine.generateLottoBetsWithOptions(selectedGame,
                        GameEngine.getCleanNumber(num), amt, opts, drawId, drawName);
                } else {
                    String num = etNumber.getText().toString().trim();
                    if (!GameEngine.validateEntry(selectedGame, num)) {
                        showToast("Nimewo pa valid"); return;
                    }
                    Bet bet = new Bet(selectedGame, num, GameEngine.getCleanNumber(num),
                        amt, drawId, drawName);
                    newBets.add(bet);
                }
                break;
        }

        if (newBets.isEmpty()) {
            showToast("Pa gen nimewo pou jeneye pari sa"); return;
        }

        cart.addAll(newBets);
        updateFreeMarriages();
        etNumber.setText("");
        etAmount.setText("");
        etNumber.requestFocus();
        renderCart();
    }

    private void updateFreeMarriages() {
        cart.removeIf(b -> b.isFree() && "special_marriage".equals(b.getFreeType()));
        double totalPayant = 0;
        for (Bet b : cart) { if (!b.isFree()) totalPayant += b.getAmount(); }
        int[][] tiers = {{100, 500, 4}, {501, 1500, 4}, {1501, -1, 4}};
        int count = GameEngine.countFreeMarriages(totalPayant, tiers);
        if (count > 0) {
            List<Bet> frees = GameEngine.generateFreeMarriages(cart, drawId, drawName, count, 2500);
            cart.addAll(frees);
        }
    }

    private void renderCart() {
        cartList.removeAllViews();
        double total = 0;
        int count = 0;

        if (cart.isEmpty()) {
            TextView empty = new TextView(this);
            empty.setText("Panye vid");
            empty.setTextColor(getResources().getColor(R.color.text_dim));
            empty.setPadding(16, 24, 16, 24);
            cartList.addView(empty);
        } else {
            LayoutInflater inf = LayoutInflater.from(this);
            for (int i = 0; i < cart.size(); i++) {
                Bet bet = cart.get(i);
                View row = inf.inflate(R.layout.item_bet_row, cartList, false);
                TextView tvGame = row.findViewById(R.id.tvGame);
                TextView tvNum = row.findViewById(R.id.tvNumber);
                TextView tvAmt = row.findViewById(R.id.tvAmount);
                View btnDel = row.findViewById(R.id.btnDelete);

                tvGame.setText(bet.getGameAbbreviation());
                tvNum.setText(bet.getNumber());
                if (bet.isFree()) {
                    tvAmt.setText("GRATIS");
                    tvAmt.setTextColor(getResources().getColor(R.color.success));
                } else {
                    tvAmt.setText((int) bet.getAmount() + " G");
                    tvAmt.setTextColor(getResources().getColor(R.color.text_main));
                }

                final int idx = i;
                btnDel.setOnClickListener(v -> { cart.remove(idx); updateFreeMarriages(); renderCart(); });

                cartList.addView(row);
                total += bet.getAmount();
                count++;
            }
        }

        tvTotal.setText((int) total + " Gdes");
        tvItemCount.setText(count + " jwèt");
    }

    private void processPrint() {
        if (cart.isEmpty()) { showToast("Panye vid — ajoute pari anvan"); return; }
        progressBar.setVisibility(View.VISIBLE);
        btnPrint.setEnabled(false);

        List<Bet> payants = new ArrayList<>();
        List<Bet> frees = new ArrayList<>();
        for (Bet b : cart) { if (b.isFree()) frees.add(b); else payants.add(b); }
        double total = 0;
        for (Bet b : payants) total += b.getAmount();

        AppSession session = AppSession.getInstance(this);
        String ticketId = "T" + System.currentTimeMillis();
        String agentId = session.getAgentId();
        String agentName = session.getAgentName();

        Gson gson = new Gson();
        JsonObject ticketData = new JsonObject();
        ticketData.addProperty("ticketId", ticketId);
        ticketData.addProperty("drawId", drawId);
        ticketData.addProperty("drawName", drawName);
        ticketData.addProperty("agentId", agentId);
        ticketData.addProperty("agentName", agentName);
        ticketData.addProperty("totalAmount", total);
        ticketData.add("bets", gson.toJsonTree(cart));
        ticketData.addProperty("date", new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.FRENCH).format(new Date()));

        ApiService.getInstance().saveTicket(ticketData, new ApiService.Callback<JsonObject>() {
            @Override
            public void onSuccess(JsonObject result) {
                String savedId = result.has("ticketId") ? result.get("ticketId").getAsString() : ticketId;
                Ticket ticket = new Ticket();
                ticket.setTicketId(savedId);
                ticket.setDrawId(drawId);
                ticket.setDrawName(drawName);
                ticket.setBets(cart);
                ticket.setTotalAmount(total);

                SunmiPrintHelper.getInstance().printTicket(ticket, "LOTATO PRO", agentName,
                    "Tikè valid pou 90 jou", "", new SunmiPrintHelper.PrintCallback() {
                        @Override public void onSuccess() {
                            progressBar.setVisibility(View.GONE);
                            btnPrint.setEnabled(true);
                            cart.clear();
                            renderCart();
                            showToast("Tikè #" + savedId + " enprime!");
                        }
                        @Override public void onError(String message) {
                            progressBar.setVisibility(View.GONE);
                            btnPrint.setEnabled(true);
                            showToast("Tikè sove, men: " + message);
                            cart.clear();
                            renderCart();
                        }
                    });
            }
            @Override
            public void onError(String error) {
                progressBar.setVisibility(View.GONE);
                btnPrint.setEnabled(true);
                showToast("Erreur: " + error);
            }
        });
    }

    private void showToast(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }
}

