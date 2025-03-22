package com.ost.application.ui.fragment.phoneinfo;

import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.hardware.fingerprint.FingerprintManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.StatFs;
import android.os.SystemClock;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import java.util.concurrent.Executor;

import androidx.annotation.NonNull;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;

import com.jaredrummler.android.device.DeviceName;
import com.ost.application.R;
import com.ost.application.databinding.FragmentDefaultInfoBinding;
import com.ost.application.ui.core.base.BaseFragment;

public class DefaultInfoFragment extends BaseFragment implements View.OnClickListener{

    private FragmentDefaultInfoBinding binding;
    private Handler handler;
    private Runnable updateRunnable;
    private long mLastClickTime;
    private int clickCount = 0;


    @SuppressLint({"UseCompatLoadingForDrawables", "ObsoleteSdkInt"})
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentDefaultInfoBinding.inflate(inflater, container, false);
        handler = new Handler(Looper.getMainLooper());
        updateRunnable = new Runnable() {
            @Override
            public void run() {
                updateInfo();
                handler.postDelayed(this, 1000);
            }
        };

        assert getActivity() != null;
        DeviceName.init(getActivity());
        DeviceName.with(getActivity()).request((info, error) -> {
            if (!isAdded()) return;

            String name = info.getName();
            String model = info.model;
            String codename = info.codename;

            binding.aboutPhoneName.setText(name);
            binding.aboutPhoneModel.setSummary(model);
            binding.aboutPhoneCodename.setSummary(codename);
        });

        handler.post(updateRunnable);

        binding.aboutPhoneFingerprintScanner.setOnClickListener(v -> showBiometricPrompt());

        return binding.getRoot();
    }

    @SuppressLint("UseCompatLoadingForDrawables")
    private void updateInfo() {
        assert getActivity() != null;
        ActivityManager actManager = (ActivityManager) getActivity().getSystemService(Context.ACTIVITY_SERVICE);
        ActivityManager.MemoryInfo memInfo = new ActivityManager.MemoryInfo();
        actManager.getMemoryInfo(memInfo);
        double availMemory = memInfo.availMem / (1024.0 * 1024 * 1024);
        double totalMemory = memInfo.totalMem / (1024.0 * 1024 * 1024);

        StatFs internalStatFs = new StatFs(Environment.getRootDirectory().getAbsolutePath());
        double internalTotal;
        double internalFree;

        StatFs externalStatFs = new StatFs(Environment.getExternalStorageDirectory().getAbsolutePath());
        double externalTotal;
        double externalFree;

        internalTotal = ((internalStatFs.getBlockCountLong() * internalStatFs.getBlockSizeLong()) / (1024.0 * 1024 * 1024));
        internalFree = (internalStatFs.getAvailableBlocksLong() * internalStatFs.getBlockSizeLong()) / (1024.0 * 1024 * 1024);
        externalTotal = (externalStatFs.getBlockCountLong() * externalStatFs.getBlockSizeLong()) / (1024.0 * 1024 * 1024);
        externalFree = (externalStatFs.getAvailableBlocksLong() * externalStatFs.getBlockSizeLong()) / (1024.0 * 1024 * 1024);

        double total = internalTotal + externalTotal;
        double free = internalFree + externalFree;
        double used = total - free;

        @SuppressLint("DefaultLocale") String availMemoryString = String.format("%.1f", availMemory);
        @SuppressLint("DefaultLocale") String totalMemoryString = String.format("%.1f", totalMemory);
        @SuppressLint("DefaultLocale") String totalString = String.format("%.1f", total);
        @SuppressLint("DefaultLocale") String freeString = String.format("%.1f", free);
        @SuppressLint("DefaultLocale") String usedString = String.format("%.1f", used);

        binding.aboutPhoneAndroid.setSummary(Build.VERSION.RELEASE);
        binding.aboutPhoneBrand.setSummary(Build.BRAND);
        binding.aboutPhoneBoard.setSummary(Build.BOARD);
        binding.aboutPhoneBuildNumber.setSummary(getBuildNumber());
        binding.aboutPhoneSdk.setSummary(Build.VERSION.SDK);
        if (getSystemProperty("ro.build.characteristics").equals("phone")) {
            binding.aboutPhoneDevice.setSummary(getString(R.string.phone));
        } else if (getSystemProperty("ro.build.characteristics").equals("tablet")) {
            binding.aboutPhoneDevice.setSummary(getString(R.string.tablet));
        } else {
            binding.aboutPhoneDevice.setSummary(getString(R.string.device) + " (" + getSystemProperty("ro.build.characteristics") + ")");
        }
        binding.aboutPhoneRam.setSummary(getString(R.string.available) + ": " + availMemoryString + " " + getString(R.string.gb) + "\n" + getString(R.string.total) + ": " + totalMemoryString + " " + getString(R.string.gb));
        binding.aboutPhoneRom.setSummary(getString(R.string.total) + ": " + totalString + " " + getString(R.string.gb) + "\n" + getString(R.string.available) + ": " + freeString + " " + getString(R.string.gb) + "\n" + getString(R.string.used) + ": " + usedString + " " + getString(R.string.gb));
        binding.aboutPhoneFingerprint.setSummary(Build.FINGERPRINT);

        if (getSystemProperty("ro.build.characteristics").equals("phone")) {
            binding.aboutPhoneImage.setImageDrawable(getResources().getDrawable(dev.oneuiproject.oneui.R.drawable.ic_oui_device_outline));
        } else if (getSystemProperty("ro.build.characteristics").equals("tablet")) {
            binding.aboutPhoneImage.setImageDrawable(getResources().getDrawable(R.drawable.ic_oui_tablet_outline));
        } else {
            binding.aboutPhoneImage.setImageDrawable(getResources().getDrawable(dev.oneuiproject.oneui.R.drawable.ic_oui_page_settings));
        }

        FingerprintManager fingerprintManager = getActivity().getSystemService(FingerprintManager.class);

        if (fingerprintManager != null && fingerprintManager.isHardwareDetected()) {
            if (fingerprintManager.hasEnrolledFingerprints()) {
                binding.aboutPhoneFingerprintScanner.setSummary(getString(R.string.support) + "\n" + getString(R.string.fingers_registered));
            } else {
                binding.aboutPhoneFingerprintScanner.setSummary(getString(R.string.support) + "\n" + getString(R.string.fingers_not_registered));
            }
        } else {
            binding.aboutPhoneFingerprintScanner.setSummary(getString(R.string.unsupport));
        }
        binding.aboutPhoneAndroid.setOnClickListener(this);
    }

    @SuppressLint("PrivateApi")
    public String getSystemProperty(String key) {
        String value = null;
        try {
            value = (String) Class.forName("android.os.SystemProperties")
                    .getMethod("get", String.class).invoke(null, key);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return value;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        handler.removeCallbacks(updateRunnable);
    }

    @Override
    public void onClick(View v) {
        clickCount++;
        handler.removeCallbacks(resetClickCountRunnable);

        if (clickCount == 3) {
            performAction();
            clickCount = 0;
        } else {
            int maxClickTime = 1000;
            handler.postDelayed(resetClickCountRunnable, maxClickTime);
        }
    }
    private void showBiometricPrompt() {
        assert getActivity() != null;
        Executor executor = ContextCompat.getMainExecutor(getActivity());
        BiometricPrompt biometricPrompt = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            biometricPrompt = new BiometricPrompt(this, executor, new BiometricPrompt.AuthenticationCallback() {
                @Override
                public void onAuthenticationSucceeded(@NonNull BiometricPrompt.AuthenticationResult result) {
                    super.onAuthenticationSucceeded(result);
                    Toast.makeText(getActivity(), getString(R.string.success), Toast.LENGTH_SHORT).show();
                }

                @Override
                public void onAuthenticationError(int errorCode, @NonNull CharSequence errString) {
                    super.onAuthenticationError(errorCode, errString);
                    Toast.makeText(getActivity(), errString, Toast.LENGTH_SHORT).show();
                }

                @Override
                public void onAuthenticationFailed() {
                    super.onAuthenticationFailed();
                    Toast.makeText(getActivity(),getString(R.string.fail), Toast.LENGTH_SHORT).show();
                }
            });
        }

        BiometricPrompt.PromptInfo promptInfo = new BiometricPrompt.PromptInfo.Builder()
                .setTitle("Test fingerprint scanner")
                .setNegativeButtonText(getString(android.R.string.cancel))
                .build();

        biometricPrompt.authenticate(promptInfo);
    }

    private void performAction() {
        long uptimeMillis = SystemClock.uptimeMillis();
        if (uptimeMillis - mLastClickTime > 600L) {
            String activity = null;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                activity = "com.android.egg.landroid.MainActivity";
            } else if (Build.VERSION.SDK_INT == Build.VERSION_CODES.Q) {
                activity = "com.android.egg.quares.QuaresActivity";
            } else if (Build.VERSION.SDK_INT == Build.VERSION_CODES.P) {
                activity = "com.android.egg.paint.PaintActivity";
            } else {
                Toast.makeText(getActivity(), getString(R.string.easter_egg_not_founded), Toast.LENGTH_SHORT).show();
            }
            if (activity != null) {
                startActivity(Intent.makeMainActivity(new ComponentName("com.android.egg", activity)));
            }
        }
    }

    public String getBuildNumber() {
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
            return getSystemProperty("ro.build.id");
        } else {
            return getSystemProperty("ro.system.build.id");
        }
    }

    private final Runnable resetClickCountRunnable = () -> clickCount = 0;

    @Override
    public int getLayoutResId() {
        return R.layout.fragment_default_info;
    }

    @Override
    public int getIconResId() {
        if (getSystemProperty("ro.build.characteristics").equals("phone")) {
            return dev.oneuiproject.oneui.R.drawable.ic_oui_device_outline;
        } else if (getSystemProperty("ro.build.characteristics").equals("tablet")) {
            return R.drawable.ic_oui_tablet_outline;
        }
        return dev.oneuiproject.oneui.R.drawable.ic_oui_page_settings;
    }

    @Override
    public CharSequence getTitle() {
        if (getSystemProperty("ro.build.characteristics").equals("phone")) {
            return getString(R.string.about_phone);
        } else if (getSystemProperty("ro.build.characteristics").equals("tablet")) {
            return getString(R.string.about_tablet);
        } else {
            return getString(R.string.about_device);
        }
    }
}
