package ua.napps.scorekeeper.dice;

import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.annotation.SuppressLint;
import android.content.Context;
import android.hardware.SensorManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.IntRange;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.Group;
import androidx.dynamicanimation.animation.DynamicAnimation;
import androidx.dynamicanimation.animation.SpringAnimation;
import androidx.dynamicanimation.animation.SpringForce;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.transition.TransitionManager;

import java.util.ArrayList;

import timber.log.Timber;
import ua.napps.scorekeeper.R;
import ua.napps.scorekeeper.settings.LocalSettings;

public class DicesFragment extends Fragment {

    private static final String ARG_CURRENT_DICE_ROLL = "ARG_CURRENT_DICE_ROLL";
    private static final String ARG_PREVIOUS_DICE_ROLL = "ARG_PREVIOUS_DICE_ROLL";

    private float accel;
    private float accelCurrent;
    private float accelLast;
    private int previousRoll;
    private int currentRoll;
    private TextView previousRollTextView;
    private TextView diceVariantInfo;
    private TextView previousRollTextViewLabel;
    private Group emptyStateGroup;
    private TextView diceTextView;
    private TextView diceCompositionTextView;
    private SpringForce springForce;
    private DiceViewModel viewModel;
    private ConstraintLayout root;
    private OnDiceFragmentInteractionListener listener;
    private MediaPlayer mp;
    private boolean soundRollEnabled;
    private int maxSide;
    private int diceCount;

    public DicesFragment() {
        // Required empty public constructor
    }

    public static DicesFragment newInstance(int lastDiceRoll, int previousDiceRoll) {
        DicesFragment fragment = new DicesFragment();
        Bundle args = new Bundle(2);
        args.putInt(ARG_CURRENT_DICE_ROLL, lastDiceRoll);
        args.putInt(ARG_PREVIOUS_DICE_ROLL, previousDiceRoll);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            currentRoll = getArguments().getInt(ARG_CURRENT_DICE_ROLL);
            previousRoll = getArguments().getInt(ARG_PREVIOUS_DICE_ROLL);
        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View contentView = inflater.inflate(R.layout.fragment_dices, container, false);
        previousRollTextView = contentView.findViewById(R.id.tv_previous_roll);
        previousRollTextViewLabel = contentView.findViewById(R.id.tv_previous_roll_label);
        diceVariantInfo = contentView.findViewById(R.id.tv_dice_variant_info);
        emptyStateGroup = contentView.findViewById(R.id.empty_state_group);
        diceTextView = contentView.findViewById(R.id.dice);
        diceCompositionTextView = contentView.findViewById(R.id.tv_dice_composition);
        root = contentView.findViewById(R.id.container);
        contentView.findViewById(R.id.iv_dice_menu).setOnClickListener(v -> showBottomSheet());

        maxSide = LocalSettings.getDiceMaxSide();
        diceCount = LocalSettings.getDiceCount();
        diceVariantInfo.setText(diceCount + "d" + maxSide);

        root.setOnClickListener(v -> viewModel.rollDice());
        root.setOnLongClickListener(v -> {
            showBottomSheet();
            return true;
        });

        diceVariantInfo.setOnClickListener(v -> showBottomSheet());

        soundRollEnabled = LocalSettings.isSoundRollEnabled();
        if (soundRollEnabled) {
            mp = MediaPlayer.create(requireActivity(), R.raw.dice_roll);
        }

        springForce = new SpringForce()
                .setDampingRatio(SpringForce.DAMPING_RATIO_MEDIUM_BOUNCY)
                .setStiffness(SpringForce.STIFFNESS_VERY_LOW)
                .setFinalPosition(1);

        observeData();
        if (LocalSettings.isShakeToRollEnabled()) {
            initSensorData();
        }
        return contentView;
    }

    private void showBottomSheet() {
        DiceBottomSheetFragment bottomSheet = new DiceBottomSheetFragment();
        bottomSheet.show(getParentFragmentManager(), "DiceBottomSheetFragment");
        bottomSheet.setOnDismissListener(d -> updateOnDismiss());
    }

    private void updateOnDismiss() {
        int ms = LocalSettings.getDiceMaxSide();
        int dc = LocalSettings.getDiceCount();

        if (diceCount != dc) {
            ObjectAnimator bounceAnimate =
                    ObjectAnimator.ofPropertyValuesHolder(diceVariantInfo,
                            PropertyValuesHolder.ofFloat("scaleX", 1.0f, 1.54f, 1.0f),
                            PropertyValuesHolder.ofFloat("scaleY", 1.0f, 1.54f, 1.0f));
            bounceAnimate.setStartDelay(100);
            bounceAnimate.start();
            diceCount = dc;
            viewModel.setDiceCount(diceCount);
            diceVariantInfo.setText(diceCount + "d" + maxSide);
        }
        if (maxSide != ms) {
            ObjectAnimator bounceAnimate =
                    ObjectAnimator.ofPropertyValuesHolder(diceVariantInfo,
                            PropertyValuesHolder.ofFloat("scaleX", 1.0f, 1.54f, 1.0f),
                            PropertyValuesHolder.ofFloat("scaleY", 1.0f, 1.54f, 1.0f));
            bounceAnimate.setStartDelay(100);
            bounceAnimate.start();
            maxSide = ms;
            viewModel.setDiceMaxSide(maxSide);
            diceVariantInfo.setText(diceCount + "d" + maxSide);
        }
        soundRollEnabled = LocalSettings.isSoundRollEnabled();
        if (!LocalSettings.isShakeToRollEnabled()) {
            viewModel.getSensorLiveData(getActivity()).removeObservers(getViewLifecycleOwner());
        } else {
            if (viewModel.getSensorLiveData(getActivity()) == null) {
                initSensorData();
            }
        }
    }


    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (context instanceof OnDiceFragmentInteractionListener) {
            listener = (OnDiceFragmentInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString() + " must implement OnDiceFragmentInteractionListener");
        }
    }

    private void initSensorData() {
        accel = 0.00f;
        accelCurrent = SensorManager.GRAVITY_EARTH;
        accelLast = SensorManager.GRAVITY_EARTH;
        viewModel.getSensorLiveData(getActivity()).observe(getViewLifecycleOwner(), se -> {
            if (se == null) {
                return;
            }

            float x = se.values[0];
            float y = se.values[1];
            float z = se.values[2];
            accelLast = accelCurrent;
            accelCurrent = (float) Math.sqrt(x * x + y * y + z * z);
            float delta = accelCurrent - accelLast;
            accel = accel * 0.9f + delta; // perform low-cut filter
            if (accel > 5.0) {
                viewModel.rollDice();
            }
        });
    }

    private void rollDice(@IntRange(from = 1, to = 100) int roll, ArrayList<Integer> rolls) {
        if (soundRollEnabled && mp != null) {
            mp.start();
        }

        updateLastRollLabel();
        Timber.d("-------------");
        Timber.d("previous Roll = %d", previousRoll);
        Timber.d("current Roll = %d", previousRoll);
        Timber.d("new Roll = %d", roll);
        previousRoll = roll;
        currentRoll = roll;
        listener.updateCurrentRoll(currentRoll);
        new SpringAnimation(diceTextView, DynamicAnimation.ROTATION)
                .setSpring(springForce)
                .setStartValue(1000f)
                .setStartVelocity(1000)
                .start();
        new SpringAnimation(diceTextView, DynamicAnimation.SCALE_X)
                .setStartValue(0.5f)
                .setSpring(springForce)
                .start();
        new SpringAnimation(diceTextView, DynamicAnimation.ALPHA)
                .setStartValue(0.1f)
                .setSpring(springForce)
                .start();
        new SpringAnimation(diceTextView, DynamicAnimation.SCALE_Y)
                .setStartValue(0.5f)
                .setSpring(springForce)
                .start();

        diceTextView.setText("" + roll);

        if (diceCompositionTextView == null) {
            return;
        }
        if (rolls.size() > 1) {
            StringBuilder composition = new StringBuilder();
            for (int i = 0; i < rolls.size(); i++) {
                composition.append(rolls.get(i));
                if (i < (rolls.size() - 1)) {
                    composition.append(" + ");
                }
            }
            diceCompositionTextView.setText(composition.toString());
            diceCompositionTextView.setVisibility(View.VISIBLE);
        } else {
            diceCompositionTextView.setVisibility(View.GONE);
        }

    }

    @SuppressLint("SetTextI18n")
    private void updateLastRollLabel() {
        TransitionManager.beginDelayedTransition(root);
        emptyStateGroup.setVisibility(View.GONE);
        diceTextView.setVisibility(View.VISIBLE);
        if (previousRoll != 0) {
            previousRollTextViewLabel.setVisibility(View.VISIBLE);
            previousRollTextView.setVisibility(View.VISIBLE);
            previousRollTextView.setText("" + previousRoll);
            ObjectAnimator scaleArrowAnimator =
                    ObjectAnimator.ofPropertyValuesHolder(previousRollTextView,
                            PropertyValuesHolder.ofFloat("scaleX", 1.0f, 1.54f, 1.0f),
                            PropertyValuesHolder.ofFloat("scaleY", 1.0f, 1.54f, 1.0f));
            scaleArrowAnimator.start();
        }
    }

    private void observeData() {
        DiceViewModelFactory factory = new DiceViewModelFactory(maxSide, diceCount);
        viewModel = new ViewModelProvider(this, factory).get(DiceViewModel.class);
        final DiceLiveData diceLiveData = viewModel.getDiceLiveData();
        diceLiveData.observe(getViewLifecycleOwner(), roll -> {
            if (roll != null && roll > 0) {
                rollDice(roll, diceLiveData.getRolls());
            } else if (currentRoll > 0) {
                updateLastRollLabel();
                diceTextView.setText("" + currentRoll);
            }
        });
    }

    @Override
    public void onDetach() {
        super.onDetach();
        listener = null;
        if (mp != null) {
            mp.release();
        }
    }

}
