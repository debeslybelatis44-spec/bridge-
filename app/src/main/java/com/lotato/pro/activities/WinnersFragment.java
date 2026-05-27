package com.lotato.pro.activities;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.lotato.pro.R;
import com.lotato.pro.api.ApiService;
import com.lotato.pro.models.AppSession;

public class WinnersFragment extends Fragment {

    private LinearLayout listContainer;
    private TextView tvTotalWinAmount;
    private View progressBar;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_winners, container, false);
        listContainer = view.findViewById(R.id.listContainer);
        tvTotalWinAmount = view.findViewById(R.id.tvTotalWinAmount);
        progressBar = view.findViewById(R.id.progressBar);

        view.findViewById(R.id.btnRefresh).setOnClickListener(v -> loadWinners());
        loadWinners();
        return view;
    }

    private void loadWinners() {
        progressBar.setVisibility(View.VISIBLE);
        String agentId = AppSession.getInstance(requireContext()).getAgentId();

        ApiService.getInstance().getWinners(agentId, new ApiService.Callback<JsonObject>() {
            @Override
            public void onSuccess(JsonObject result) {
                progressBar.setVisibility(View.GONE);
                JsonArray winners = result.has("winners") ? result.getAsJsonArray("winners") : new JsonArray();
                double total = 0;
                listContainer.removeAllViews();

                if (winners.size() == 0) {
                    TextView empty = new TextView(getContext());
                    empty.setText("Okenn tikè ganyen jodi a");
                    empty.setTextColor(getResources().getColor(R.color.text_dim));
                    empty.setPadding(0, 40, 0, 0);
                    listContainer.addView(empty);
                } else {
                    LayoutInflater inf = LayoutInflater.from(getContext());
                    for (JsonElement el : winners) {
                        JsonObject w = el.getAsJsonObject();
                        View card = inf.inflate(R.layout.item_winner_card, listContainer, false);
                        TextView tvId = card.findViewById(R.id.tvTicketId);
                        TextView tvDraw = card.findViewById(R.id.tvDrawName);
                        TextView tvWin = card.findViewById(R.id.tvWinAmount);
                        TextView tvPaid = card.findViewById(R.id.tvPaidStatus);

                        String ticketId = w.has("ticket_id") ? w.get("ticket_id").getAsString() : "?";
                        String draw = w.has("draw_name") ? w.get("draw_name").getAsString() : "";
                        double winAmount = w.has("win_amount") ? w.get("win_amount").getAsDouble() : 0;
                        boolean paid = w.has("paid") && w.get("paid").getAsBoolean();

                        tvId.setText("#" + ticketId);
                        tvDraw.setText(draw);
                        tvWin.setText((int) winAmount + " G");
                        tvPaid.setText(paid ? "PEYE" : "PA PEYE");
                        tvPaid.setTextColor(paid ?
                            getResources().getColor(R.color.success) :
                            getResources().getColor(R.color.warning));

                        total += winAmount;
                        listContainer.addView(card);
                    }
                }
                tvTotalWinAmount.setText((int) total + " G");
            }

            @Override
            public void onError(String error) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(getContext(), "Erreur: " + error, Toast.LENGTH_SHORT).show();
            }
        });
    }
}

