package org.xvdr.robotv.setup;

import android.app.FragmentManager;
import android.graphics.drawable.Drawable;
import android.media.AudioFormat;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v17.leanback.app.GuidedStepFragment;
import android.support.v17.leanback.widget.GuidanceStylist;
import android.support.v17.leanback.widget.GuidedAction;

import com.google.android.exoplayer.audio.AudioCapabilities;

import org.xvdr.robotv.R;

import java.util.List;

public class SetupFragment extends GuidedStepFragment {

    static final String TAG = "SetupFragment";

    static final int ACTION_SERVER = 1;
    static final int ACTION_LANGUAGE = 2;
    static final int ACTION_REFRESHRATE = 3;
    static final int ACTION_IMPORT = 4;
    static final int ACTION_PASSTHROUGH = 6;
    static final int ACTION_SPEAKERCONFIG = 7;
    static final int ACTION_TIMESHIFT = 8;

    private GuidedAction mActionServer;
    private GuidedAction mActionLanguage;
    private GuidedAction mActionRefreshRate;
    private GuidedAction mActionPassthrough;
    private GuidedAction mActionSpeakerConfig;
    private GuidedAction mActionTimeshift;

    @NonNull
    @Override
    public GuidanceStylist.Guidance onCreateGuidance(Bundle savedInstanceState) {
        String title = getString(R.string.setup_root_title);
        String breadcrumb = "";
        String description = getString(R.string.setup_root_desc);
        Drawable icon = getActivity().getDrawable(R.drawable.ic_live_tv_white_48dp);

        return new GuidanceStylist.Guidance(title, description, breadcrumb, icon);
    }

    @Override
    public void onCreateButtonActions(@NonNull List<GuidedAction> actions, Bundle savedInstanceState) {
    }

    @Override
    public void onCreateActions(@NonNull List<GuidedAction> actions, Bundle savedInstanceState) {
        String server = SetupUtils.getServer(getActivity());

        mActionServer = new GuidedAction.Builder(getActivity())
        .id(ACTION_SERVER)
        .title(server)
        .description(getString(R.string.setup_root_server_desc))
        .editable(true)
        .build();

        mActionLanguage = new GuidedAction.Builder(getActivity())
        .id(ACTION_LANGUAGE)
        .title(getString(R.string.setup_root_language_title))
        .build();

        mActionRefreshRate = new GuidedAction.Builder(getActivity())
        .id(ACTION_REFRESHRATE)
        .title(getString(R.string.setup_root_refreshrate_title))
        .build();

        mActionPassthrough = new GuidedAction.Builder(getActivity())
        .id(ACTION_PASSTHROUGH)
        .title(getString(R.string.setup_root_passthrough_title))
        .checkSetId(GuidedAction.CHECKBOX_CHECK_SET_ID)
        .checked(SetupUtils.getPassthrough(getActivity()))
        .build();

        mActionSpeakerConfig = new GuidedAction.Builder(getActivity())
        .id(ACTION_SPEAKERCONFIG)
        .title(getString(R.string.setup_root_speakerconfig_title))
        .build();

        mActionTimeshift = new GuidedAction.Builder(getActivity())
        .id(ACTION_TIMESHIFT)
        .title(getString(R.string.setup_root_timeshift_title))
        .checkSetId(GuidedAction.CHECKBOX_CHECK_SET_ID)
        .checked(SetupUtils.getTimeshiftEnabled(getActivity()))
        .build();

        actions.add(mActionServer);
        actions.add(mActionLanguage);

        AudioCapabilities audioCapabilities = AudioCapabilities.getCapabilities(getActivity());

        if(audioCapabilities.supportsEncoding(AudioFormat.ENCODING_AC3)) {
            actions.add(mActionPassthrough);
        }

        actions.add(mActionSpeakerConfig);

        actions.add(mActionTimeshift);

        if(SetupUtils.isRefreshRateChangeSupported()) {
            actions.add(mActionRefreshRate);
        }

        actions.add(new GuidedAction.Builder(getActivity())
                    .id(ACTION_IMPORT)
                    .title(getString(R.string.setup_root_import_title))
                    .description(getString(R.string.setup_root_import_desc))
                    .hasNext(true)
                    .build());
    }

    @Override
    public void onResume() {
        super.onResume();

        // language

        int langIndex = SetupUtils.getLanguageIndex(getActivity());
        String[] langArray = getResources().getStringArray(R.array.languages_array);

        if(langIndex > langArray.length - 1) {
            langIndex = 0;
        }

        mActionLanguage.setDescription(langArray[langIndex]);

        // refresh rate
        String[] refreshRates = getResources().getStringArray(R.array.refresh_rate_name_array);
        int refreshRateIndex = SetupUtils.getRefreshRateIndex(getActivity());

        mActionRefreshRate.setDescription(refreshRates[refreshRateIndex]);

        boolean passthrough = SetupUtils.getPassthrough(getActivity());
        mActionPassthrough.setChecked(passthrough);

        int speakerConfig = SetupUtils.getSpeakerConfiguration(getActivity());
        mActionSpeakerConfig.setDescription(
            (speakerConfig == 6) ? getString(R.string.setup_root_speakerconfig_digital_51) :
            (speakerConfig == 4) ? getString(R.string.setup_root_speakerconfig_surround) :
            (speakerConfig == 2) ? getString(R.string.setup_root_speakerconfig_stereo) :
            "");
        mActionSpeakerConfig.setEnabled(!passthrough);

        boolean timeshift = SetupUtils.getTimeshiftEnabled(getActivity());
        mActionTimeshift.setChecked(timeshift);
    }

    public void onGuidedActionClicked(GuidedAction action) {
        FragmentManager fm = getFragmentManager();

        switch((int)action.getId()) {
            case ACTION_SERVER:
                SetupUtils.setServer(getActivity(), mActionServer.getTitle().toString());
                break;

            case ACTION_LANGUAGE:
                GuidedStepFragment.add(fm, new SetupFragmentLanguage());
                break;

            case ACTION_REFRESHRATE:
                GuidedStepFragment.add(fm, new SetupFragmentRefreshRate());
                break;

            case ACTION_PASSTHROUGH:
                mActionSpeakerConfig.setEnabled(!action.isChecked());
                notifyActionChanged(findActionPositionById(ACTION_SPEAKERCONFIG));
                SetupUtils.setPassthrough(getActivity(), action.isChecked());
                break;

            case ACTION_SPEAKERCONFIG:
                GuidedStepFragment.add(fm, new SetupFragmentSpeakerConfig());
                break;

            case ACTION_TIMESHIFT:
                SetupUtils.setTimeshiftEnabled(getActivity(), action.isChecked());
                break;

            case ACTION_IMPORT:
                importChannels();
                break;
        }
    }

    private void importChannels() {
        SetupFragmentImport progress = new SetupFragmentImport();

        getFragmentManager().beginTransaction().add(android.R.id.content, progress).commit();

        SetupActivity activity = (SetupActivity) getActivity();
        activity.registerChannels(progress);
    }
}
