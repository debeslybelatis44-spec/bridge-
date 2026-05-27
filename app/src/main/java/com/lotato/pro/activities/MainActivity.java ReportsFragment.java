package com.lotato.pro.activities;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.gson.JsonObject;
import com.lotato.pro.R;
import com.lotato.pro.api.ApiService;
import com.lotato.pro.models.AppSession;
import com.lotato.pro.print.SunmiPrintHelper;

public class ReportsFragment extends Fragment {

    private TextView tvTotalTickets, tvTotalBets, tvTotalWins, tvBalance;
    private Button btnPrint, btnRefresh;
    private View progressBar;
    private double currentBets, currentWins, currentBalance;
    private int currentTickets;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_reports, container, false);

        tvTotalTickets = view.findViewById(R.id.tvTotalTickets);
        tvTotalBets = view.findViewById(R.id.tvTotalBets);
        tvTotalWins = view.findViewById(R.id.tvTotalWins);
        tvBalance = view.findViewById(R.id.tvBalance);
        btnPrint = view.findViewById(R.id.btnPrintReport);
        btnRefresh = view.findViewById(R.id.btnRefresh);
        progressBar = view.findViewById(R.id.progressBar);

        btnRefresh.setOnClickListener(v -> loadReports());
        btnPrint.setOnClickListener(v -> printReport());

        loadReports();
        return view;
    }

    private void loadReports() {
        progressBar.setVisibility(View.VISIBLE);
        String agentId = AppSession.getInstance(requireContext()).getAgentId();

        ApiService.getInstance().getReports(agentId, new ApiService.Callback<JsonObject>() {
            @Override
            public void onSuccess(JsonObject result) {
                progressBar.setVisibility(View.GONE);
                currentTickets = result.has("totalTickets") ? result.get("totalTickets").getAsInt() : 0;
                currentBets = result.has("totalBets") ? result.get("totalBets").getAsDouble() : 0;
                currentWins = result.has("totalWins") ? result.get("totalWins").getAsDouble() : 0;
                currentBalance = result.has("balance") ? result.get("balance").getAsDouble() : (currentBets - currentWins);

                tvTotalTickets.setText(String.valueOf(currentTickets));
                tvTotalBets.setText((int) currentBets + " G");
                tvTotalWins.setText((int) currentWins + " G");
                tvBalance.setText((int) currentBalance + " G");

                if (currentBalance >= 0) {
                    tvBalance.setTextColor(getResources().getColor(R.color.success));
                } else {
                    tvBalance.setTextColor(getResources().getColor(R.color.danger));
                }
            }

            @Override
            public void onError(String error) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(getContext(), "Erreur: " + error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void printReport() {
        String agentName = AppSession.getInstance(requireContext()).getAgentName();
        SunmiPrintHelper.getInstance().printReport(agentName, currentTickets, currentBets, currentWins,
            currentBalance, new SunmiPrintHelper.PrintCallback() {
                @Override public void onSuccess() {
                    Toast.makeText(getContext(), "Rapò enprime!", Toast.LENGTH_SHORT).show();
                }
                @Override public void onError(String message) {
                    Toast.makeText(getContext(), message, Toast.LENGTH_LONG).show();
                }
            });
    }
}

