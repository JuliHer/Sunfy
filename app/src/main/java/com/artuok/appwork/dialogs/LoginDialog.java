package com.artuok.appwork.dialogs;

import android.app.AlertDialog;
import android.app.Dialog;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.artuok.appwork.R;

import java.util.ArrayList;
import java.util.Arrays;

public class LoginDialog extends DialogFragment {

    Spinner spinner;
    EditText cc;
    EditText phoneEdit;
    LinearLayout waiting, input;
    TextView positiveBtn, negativeBtn, entercode;
    int lastLength = 0;

    private onResponseListener negative;
    private onResponseListener positive;
    private onResponseListener enter;

    public void setNegative(onResponseListener negative) {
        this.negative = negative;
    }

    public void setPositive(onResponseListener positive) {
        this.positive = positive;
    }

    public void setEnterCode(onResponseListener enter){
        this.enter = enter;
    }

    public void setWaiting(boolean b) {
        if (b) {
            waiting.setVisibility(View.VISIBLE);
            input.setVisibility(View.GONE);
        } else {
            waiting.setVisibility(View.GONE);
            input.setVisibility(View.VISIBLE);
        }
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireActivity());
        LayoutInflater inflater = requireActivity().getLayoutInflater();
        View root = inflater.inflate(R.layout.dialog_login_layout, null);

        spinner = root.findViewById(R.id.country);
        cc = root.findViewById(R.id.countryCodes);
        phoneEdit = root.findViewById(R.id.editTextPhone);
        input = root.findViewById(R.id.input);
        waiting = root.findViewById(R.id.waiting);
        positiveBtn = root.findViewById(R.id.positive);
        negativeBtn = root.findViewById(R.id.negative);
        entercode = root.findViewById(R.id.enterCode);

        String[] array = {"", "93", "355", "213", "1684", "376", "244", "1264", "672", "1268", "54", "374", "297", "61", "43", "994", "1242", "973", "880", "1246", "375", "32", "501", "229", "1441", "975", "591", "387", "267", "55", "246", "1284", "673", "359", "226", "257", "855", "237", "1", "238", "1345", "236", "235", "56", "86", "61", "61", "57", "269", "682", "506", "385", "53", "599", "357", "420", "243", "45", "253", "1767", "1809", "670", "593", "20", "503", "240", "291", "372", "251", "500", "298", "679", "358", "33", "689", "241", "220", "995", "49", "233", "350", "30", "299", "1473", "1671", "502", "441481", "224", "245", "592", "509", "504", "852", "36", "354", "91", "62", "98", "964", "353", "441624", "972", "39", "225", "1876", "81", "441534", "962", "7", "254", "686", "383", "965", "996", "856", "371", "961", "266", "231", "218", "423", "370", "352", "853", "389", "261", "265", "60", "960", "223", "356", "692", "222", "230", "262", "52", "691", "373", "377", "976", "382", "1664", "212", "258", "95", "264", "674", "977", "31", "599", "687", "64", "505", "227", "234", "683", "850", "1670", "47", "968", "92", "680", "970", "507", "675", "595", "51", "63", "64", "48", "351", "1787", "974", "242", "262", "40", "7", "250", "590", "290", "1869", "1758", "590", "508", "1784", "685", "378", "239", "966", "221", "381", "248", "232", "65", "1721", "421", "386", "677", "252", "27", "82", "211", "34", "94", "249", "597", "47", "268", "46", "41", "963", "886", "992", "255", "66", "228", "690", "676", "1868", "216", "90", "993", "1649", "688", "1340", "256", "380", "971", "44", "1", "598", "998", "678", "379", "58", "84", "681", "212", "967", "260", "263"};
        String[] countryCodes = {"", "AF", "AL", "DZ", "AS", "AD", "AO", "AI", "AQ", "AG", "AR", "AM", "AW", "AU", "AT", "AZ", "BS", "BH", "BD", "BB", "BY", "BE", "BZ", "BJ", "BM", "BT", "BO", "BA", "BW", "BR", "IO", "VG", "BN", "BG", "BF", "BI", "KH", "CM", "CA", "CV", "KY", "CF", "TD", "CL", "CN", "CX", "CC", "CO", "KM", "CK", "CR", "HR", "CU", "CW", "CY", "CZ", "CD", "DK", "DJ", "DM", "DO", "TL", "EC", "EG", "SV", "GQ", "ER", "EE", "ET", "FK", "FO", "FJ", "FI", "FR", "PF", "GA", "GM", "GE", "DE", "GH", "GI", "GR", "GL", "GD", "GU", "GT", "GG", "GN", "GW", "GY", "HT", "HN", "HK", "HU", "IS", "IN", "ID", "IR", "IQ", "IE", "IM", "IL", "IT", "CI", "JM", "JP", "JE", "JO", "KZ", "KE", "KI", "XK", "KW", "KG", "LA", "LV", "LB", "LS", "LR", "LY", "LI", "LT", "LU", "MO", "MK", "MG", "MW", "MY", "MV", "ML", "MT", "MH", "MR", "MU", "YT", "MX", "FM", "MD", "MC", "MN", "ME", "MS", "MA", "MZ", "MM", "NA", "NR", "NP", "NL", "AN", "NC", "NZ", "NI", "NE", "NG", "NU", "KP", "MP", "NO", "OM", "PK", "PW", "PS", "PA", "PG", "PY", "PE", "PH", "PN", "PL", "PT", "PR", "QA", "CG", "RE", "RO", "RU", "RW", "BL", "SH", "KN", "LC", "MF", "PM", "VC", "WS", "SM", "ST", "SA", "SN", "RS", "SC", "SL", "SG", "SX", "SK", "SI", "SB", "SO", "ZA", "KR", "SS", "ES", "LK", "SD", "SR", "SJ", "SZ", "SE", "CH", "SY", "TW", "TJ", "TZ", "TH", "TG", "TK", "TO", "TT", "TN", "TR", "TM", "TC", "TV", "VI", "UG", "UA", "AE", "GB", "US", "UY", "UZ", "VU", "VA", "VE", "VN", "WF", "EH", "YE", "ZM", "ZW"};
        String[] country = {"Pais", "Afghanistan", "Albania", "Algeria", "American Samoa", "Andorra", "Angola", "Anguilla", "Antarctica", "Antigua and Barbuda", "Argentina", "Armenia", "Aruba", "Australia", "Austria", "Azerbaijan", "Bahamas", "Bahrain", "Bangladesh", "Barbados", "Belarus", "Belgium", "Belize", "Benin", "Bermuda", "Bhutan", "Bolivia", "Bosnia and Herzegovina", "Botswana", "Brazil", "British Indian Ocean Territory", "British Virgin Islands", "Brunei", "Bulgaria", "Burkina Faso", "Burundi", "Cambodia", "Cameroon", "Canada", "Cape Verde", "Cayman Islands", "Central African Republic", "Chad", "Chile", "China", "Christmas Island", "Cocos Islands", "Colombia", "Comoros", "Cook Islands", "Costa Rica", "Croatia", "Cuba", "Curacao", "Cyprus", "Czech Republic", "Democratic Republic of the Congo", "Denmark", "Djibouti", "Dominica", "Dominican Republic", "East Timor", "Ecuador", "Egypt", "El Salvador", "Equatorial Guinea", "Eritrea", "Estonia", "Ethiopia", "Falkland Islands", "Faroe Islands", "Fiji", "Finland", "France", "French Polynesia", "Gabon", "Gambia", "Georgia", "Germany", "Ghana", "Gibraltar", "Greece", "Greenland", "Grenada", "Guam", "Guatemala", "Guernsey", "Guinea", "Guinea Bissau", "Guyana", "Haiti", "Honduras", "Hong Kong", "Hungary", "Iceland", "India", "Indonesia", "Iran", "Iraq", "Ireland", "Isle of Man", "Israel", "Italy", "Ivory Coast", "Jamaica", "Japan", "Jersey", "Jordan", "Kazakhstan", "Kenya", "Kiribati", "Kosovo", "Kuwait", "Kyrgyzstan", "Laos", "Latvia", "Lebanon", "Lesotho", "Liberia", "Libya", "Liechtenstein", "Lithuania", "Luxembourg", "Macao", "Macedonia", "Madagascar", "Malawi", "Malaysia", "Maldives", "Mali", "Malta", "Marshall Islands", "Mauritania", "Mauritius", "Mayotte", "Mexico", "Micronesia", "Moldova", "Monaco", "Mongolia", "Montenegro", "Montserrat", "Morocco", "Mozambique", "Myanmar", "Namibia", "Nauru", "Nepal", "Netherlands", "Netherlands Antilles", "New Caledonia", "New Zealand", "Nicaragua", "Niger", "Nigeria", "Niue", "North Korea", "Northern Mariana Islands", "Norway", "Oman", "Pakistan", "Palau", "Palestine", "Panama", "Papua New Guinea", "Paraguay", "Peru", "Philippines", "Pitcairn", "Poland", "Portugal", "Puerto Rico", "Qatar", "Republic of the Congo", "Reunion", "Romania", "Russia", "Rwanda", "Saint Barthelemy", "Saint Helena", "Saint Kitts and Nevis", "Saint Lucia", "Saint Martin", "Saint Pierre and Miquelon", "Saint Vincent and the Grenadines", "Samoa", "San Marino", "Sao Tome and Principe", "Saudi Arabia", "Senegal", "Serbia", "Seychelles", "Sierra Leone", "Singapore", "Sint Maarten", "Slovakia", "Slovenia", "Solomon Islands", "Somalia", "South Africa", "South Korea", "South Sudan", "Spain", "Sri Lanka", "Sudan", "Suriname", "Svalbard and Jan Mayen", "Swaziland", "Sweden", "Switzerland", "Syria", "Taiwan", "Tajikistan", "Tanzania", "Thailand", "Togo", "Tokelau", "Tonga", "Trinidad and Tobago", "Tunisia", "Turkey", "Turkmenistan", "Turks and Caicos Islands", "Tuvalu", "U.S. Virgin Islands", "Uganda", "Ukraine", "United Arab Emirates", "United Kingdom", "United States", "Uruguay", "Uzbekistan", "Vanuatu", "Vatican", "Venezuela", "Vietnam", "Wallis and Futuna", "Western Sahara", "Yemen", "Zambia", "Zimbabwe"};


        ArrayList<String> arrayList = new ArrayList<>(Arrays.asList(country));
        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireActivity(), R.layout.style_spinner, arrayList);
        spinner.setAdapter(adapter);

        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                if (i != 0) {
                    cc.setText(array[i]);
                    phoneEdit.requestFocus();
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });

        phoneEdit.setOnKeyListener((view, i, keyEvent) -> {
            if (lastLength == 0) {
                if (i == KeyEvent.KEYCODE_DEL) {
                    cc.requestFocus();
                    cc.setSelection(cc.getText().length());
                }
            }

            lastLength = phoneEdit.getText().toString().length();

            return false;
        });

        cc.setOnKeyListener((view, i, keyEvent) -> {

            if (Arrays.asList(array).contains(cc.getText().toString())) {
                int g = Arrays.asList(array).indexOf(cc.getText().toString());


                spinner.setSelection(g);
                cc.requestFocus();
            }

            return false;
        });

        entercode.setOnClickListener(view -> enter.onClick(view, null, null));


        if (negative != null) {
            negativeBtn.setVisibility(View.VISIBLE);
            negativeBtn.setOnClickListener(view -> negative.onClick(view, "", ""));
        } else {
            negativeBtn.setVisibility(View.GONE);
        }

        if (positive != null) {
            positiveBtn.setVisibility(View.VISIBLE);
            positiveBtn.setOnClickListener(view -> {
                String num = "+" + cc.getText().toString() + phoneEdit.getText().toString();
                if (Arrays.asList(array).contains(cc.getText().toString())) {
                    int g = Arrays.asList(array).indexOf(cc.getText().toString());
                    positive.onClick(view, num, countryCodes[g]);
                }
            });
        }
        builder.setView(root);
        return builder.create();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View root = super.onCreateView(inflater, container, savedInstanceState);
        getDialog().getWindow().setBackgroundDrawableResource(R.drawable.dialog_background);
        return root;
    }

    public interface onResponseListener {
        void onClick(View view, String text, String region);
    }
}
