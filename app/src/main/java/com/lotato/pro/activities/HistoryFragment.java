package com.lotato.pro.activities;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.lotato.pro.R;
import com.lotato.pro.api.ApiService;
import com.lotato.pro.models.AppSession;
import com.lotato.pro.models.Bet;
import com.lotato.pro.models.Ticket;
import com.lotato.pro.print.SunmiPrintHelper;

import java.util.ArrayList;
import java.util.List;

public class HistoryFragment extends Fragment {

    private LinearLayout listContainer;
    private EditText etSearch;
    private View progressBar;
    private List<Ticket> allTickets = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_history, container, false);
        listContainer = view.findViewById(R.id.listContainer);
        etSearch = view.findViewById(R.id.etSearch);
        progressBar = view.findViewById(R.id.progressBar);

        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterTickets(s.toString());
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        loadHistory();
        return view;
    }

    private void loadHistory() {
        progressBar.setVisibility(View.VISIBLE);
        String agentId = AppSession.getInstance(requireContext()).getAgentId();

        ApiService.getInstance().getTickets(agentId, new ApiService.Callback<List<Ticket>>() {
            @Override
            public void onSuccess(List<Ticket> result) {
                progressBar.setVisibility(View.GONE);
                allTickets = result;
                renderTickets(allTickets);
            }

            @Override
            public void onError(String error) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(getContext(), "Erreur: " + error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void filterTickets(String query) {
        if (query.isEmpty()) { renderTickets(allTickets); return; }
        List<Ticket> filtered = new ArrayList<>();
        for (Ticket t : allTickets) {
            if ((t.getTicketId() != null && t.getTicketId().contains(query)) ||
                (t.getDrawName() != null && t.getDrawName().toLowerCase().contains(query.toLowerCase()))) {
                filtered.add(t);
            }
            if (t.getBets() != null) {
                for (Bet b : t.getBets()) {
                    if (b.getNumber() != null && b.getNumber().contains(query)) {
                        filtered.add(t);
                        break;
                    }
                }
            }
        }
        renderTickets(filtered);
    }

    private void renderTickets(List<Ticket> tickets) {
        listContainer.removeAllViews();
        if (tickets.isEmpty()) {
            TextView empty = new TextView(getContext());
            empty.setText("Okenn tikè jwenn");
            empty.setTextColor(getResources().getColor(R.color.text_dim));
            empty.setPadding(0, 40, 0, 0);
            listContainer.addView(empty);
            return;
        }

        LayoutInflater inflater = LayoutInflater.from(getContext());
        for (Ticket ticket : tickets) {
            View card = inflater.inflate(R.layout.item_ticket_card, listContainer, false);
            TextView tvId = card.findViewById(R.id.tvTicketId);
            TextView tvDraw = card.findViewById(R.id.tvDrawName);
            TextView tvAmount = card.findViewById(R.id.tvAmount);
            TextView tvStatus = card.findViewById(R.id.tvStatus);
            View btnReprint = card.findViewById(R.id.btnReprint);

            tvId.setText("#" + ticket.getTicketId());
            tvDraw.setText(ticket.getDrawName());
            tvAmount.setText((int) ticket.getTotalAmount() + " G");
            tvStatus.setText(ticket.getStatusLabel());

            if (ticket.isChecked() && ticket.getWinAmount() > 0) {
                tvStatus.setTextColor(getResources().getColor(R.color.success));
            } else if (ticket.isChecked()) {
                tvStatus.setTextColor(getResources().getColor(R.color.danger));
            } else {
                tvStatus.setTextColor(getResources().getColor(R.color.warning));
            }

            btnReprint.setOnClickListener(v -> reprintTicket(ticket));
            listContainer.addView(card);
        }
    }

    private void reprintTicket(Ticket ticket) {
        String lotteryName = "LOTATO PRO";
        SunmiPrintHelper.getInstance().printReprintTicket(ticket, lotteryName, new SunmiPrintHelper.PrintCallback() {
            @Override public void onSuccess() {
                Toast.makeText(getContext(), "Tikè enprime!", Toast.LENGTH_SHORT).show();
            }
            @Override public void onError(String message) {
                Toast.makeText(getContext(), message, Toast.LENGTH_LONG).show();
            }
        });
    }
}

