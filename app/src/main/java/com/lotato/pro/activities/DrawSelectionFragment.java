package com.lotato.pro.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.GridLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.lotato.pro.R;
import com.lotato.pro.api.ApiService;
import com.lotato.pro.models.AppSession;
import com.lotato.pro.models.Draw;

import java.util.List;

public class DrawSelectionFragment extends Fragment {

    private GridLayout gridDraws;
    private View progressBar;
    private List<Draw> draws;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_draw_selection, container, false);
        gridDraws = view.findViewById(R.id.gridDraws);
        progressBar = view.findViewById(R.id.progressBar);
        loadDraws();
        return view;
    }

    private void loadDraws() {
        progressBar.setVisibility(View.VISIBLE);
        gridDraws.setVisibility(View.GONE);

        ApiService.getInstance().getDraws(new ApiService.Callback<List<Draw>>() {
            @Override
            public void onSuccess(List<Draw> result) {
                draws = result;
                progressBar.setVisibility(View.GONE);
                gridDraws.setVisibility(View.VISIBLE);
                renderDraws();
            }

            @Override
            public void onError(String error) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(getContext(), "Erreur chargement tirages: " + error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void renderDraws() {
        gridDraws.removeAllViews();
        if (draws == null) return;

        for (Draw draw : draws) {
            View card = LayoutInflater.from(getContext())
                .inflate(R.layout.item_draw_card, gridDraws, false);

            TextView tvName = card.findViewById(R.id.tvDrawName);
            TextView tvTime = card.findViewById(R.id.tvDrawTime);
            TextView tvStatus = card.findViewById(R.id.tvDrawStatus);

            tvName.setText(draw.getName());
            tvTime.setText(draw.getTime());

            if (!draw.isActive()) {
                tvStatus.setVisibility(View.VISIBLE);
                tvStatus.setText("BLOKÉ");
                card.setAlpha(0.5f);
                card.setClickable(false);
            } else {
                tvStatus.setVisibility(View.GONE);
                card.setOnClickListener(v -> openBetting(draw));
            }

            GridLayout.LayoutParams params = new GridLayout.LayoutParams();
            params.width = 0;
            params.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
            params.setMargins(8, 8, 8, 8);
            card.setLayoutParams(params);

            gridDraws.addView(card);
        }
    }

    private void openBetting(Draw draw) {
        Intent intent = new Intent(getContext(), BettingActivity.class);
        intent.putExtra("drawId", String.valueOf(draw.getId()));
        intent.putExtra("drawName", draw.getName());
        intent.putExtra("drawTime", draw.getTime());
        startActivity(intent);
    }
}

