package com.artuok.appwork

import android.os.Bundle
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthProvider

class LoginActivity : AppCompatActivity() {
    private lateinit var spinner: Spinner
    private lateinit var cc: EditText
    private lateinit var phoneEdit: EditText
    private lateinit var codeInput: EditText
    private lateinit var enterBtn: LinearLayout
    private lateinit var verifyBtn: LinearLayout
    private lateinit var userPhoneNumber: String
    private lateinit var userCodePhoneNumber: String
    private lateinit var auth: FirebaseAuth
    private lateinit var callbacks: PhoneAuthProvider.OnVerificationStateChangedCallbacks
    private var lastLength = 0
    private var storedVerificationId: String = ""
    private lateinit var login: LinearLayout
    private lateinit var verify: LinearLayout
    private lateinit var wait: LinearLayout
    private lateinit var enterCode: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)
        /*spinner = findViewById(R.id.country)
        cc = findViewById(R.id.countryCodes)
        phoneEdit = findViewById(R.id.editTextPhone)
        enterBtn = findViewById(R.id.enterBtn)
        verifyBtn = findViewById(R.id.verifyBtn)

        login = findViewById(R.id.login)
        verify = findViewById(R.id.verify)
        wait = findViewById(R.id.wait)
        codeInput = findViewById(R.id.codeHiden)
        enterCode = findViewById(R.id.enterCode)

        auth = Firebase.auth

        callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
            override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                signInWithPhoneAuthCredential(credential)
            }

            override fun onVerificationFailed(e: FirebaseException) {
                Toast.makeText(this@LoginActivity, e.message, Toast.LENGTH_SHORT).show()
            }

            override fun onCodeSent(
                verificationId: String,
                p1: PhoneAuthProvider.ForceResendingToken
            ) {


                val timeout = Calendar.getInstance().timeInMillis + 120000L
                val sharedPreferences: SharedPreferences = getSharedPreferences("chat", Context.MODE_PRIVATE)
                val editor = sharedPreferences.edit()

                wait.visibility = View.GONE
                login.visibility = View.GONE
                verify.visibility = View.VISIBLE
                editor.putLong("verifyTimeOut", timeout)
                editor.putString("VerificationId", verificationId)
                editor.apply()

                storedVerificationId = verificationId
            }
        }

        val array = arrayOf("", "93", "355", "213", "1684", "376", "244", "1264", "672", "1268", "54", "374", "297", "61", "43", "994", "1242", "973", "880", "1246", "375", "32", "501", "229", "1441", "975", "591", "387", "267", "55", "246", "1284", "673", "359", "226", "257", "855", "237", "1", "238", "1345", "236", "235", "56", "86", "61", "61", "57", "269", "682", "506", "385", "53", "599", "357", "420", "243", "45", "253", "1767", "1809", "670", "593", "20", "503", "240", "291", "372", "251", "500", "298", "679", "358", "33", "689", "241", "220", "995", "49", "233", "350", "30", "299", "1473", "1671", "502", "441481", "224", "245", "592", "509", "504", "852", "36", "354", "91", "62", "98", "964", "353", "441624", "972", "39", "225", "1876", "81", "441534", "962", "7", "254", "686", "383", "965", "996", "856", "371", "961", "266", "231", "218", "423", "370", "352", "853", "389", "261", "265", "60", "960", "223", "356", "692", "222", "230", "262", "52", "691", "373", "377", "976", "382", "1664", "212", "258", "95", "264", "674", "977", "31", "599", "687", "64", "505", "227", "234", "683", "850", "1670", "47", "968", "92", "680", "970", "507", "675", "595", "51", "63", "64", "48", "351", "1787", "974", "242", "262", "40", "7", "250", "590", "290", "1869", "1758", "590", "508", "1784", "685", "378", "239", "966", "221", "381", "248", "232", "65", "1721", "421", "386", "677", "252", "27", "82", "211", "34", "94", "249", "597", "47", "268", "46", "41", "963", "886", "992", "255", "66", "228", "690", "676", "1868", "216", "90", "993", "1649", "688", "1340", "256", "380", "971", "44", "1", "598", "998", "678", "379", "58", "84", "681", "212", "967", "260", "263")
        val countryCodes = arrayOf("", "AF", "AL", "DZ", "AS", "AD", "AO",
            "AI",
            "AQ",
            "AG",
            "AR",
            "AM",
            "AW",
            "AU",
            "AT",
            "AZ",
            "BS",
            "BH",
            "BD",
            "BB",
            "BY",
            "BE",
            "BZ",
            "BJ",
            "BM",
            "BT",
            "BO",
            "BA",
            "BW",
            "BR",
            "IO",
            "VG",
            "BN",
            "BG",
            "BF",
            "BI",
            "KH",
            "CM",
            "CA",
            "CV",
            "KY",
            "CF",
            "TD",
            "CL",
            "CN",
            "CX",
            "CC",
            "CO",
            "KM",
            "CK",
            "CR",
            "HR",
            "CU",
            "CW",
            "CY",
            "CZ",
            "CD",
            "DK",
            "DJ",
            "DM",
            "DO",
            "TL",
            "EC",
            "EG",
            "SV",
            "GQ",
            "ER",
            "EE",
            "ET",
            "FK",
            "FO",
            "FJ",
            "FI",
            "FR",
            "PF",
            "GA",
            "GM",
            "GE",
            "DE",
            "GH",
            "GI",
            "GR",
            "GL",
            "GD",
            "GU",
            "GT",
            "GG",
            "GN",
            "GW",
            "GY",
            "HT",
            "HN",
            "HK",
            "HU",
            "IS",
            "IN",
            "ID",
            "IR",
            "IQ",
            "IE",
            "IM",
            "IL",
            "IT",
            "CI",
            "JM",
            "JP",
            "JE",
            "JO",
            "KZ",
            "KE",
            "KI",
            "XK",
            "KW",
            "KG",
            "LA",
            "LV",
            "LB",
            "LS",
            "LR",
            "LY",
            "LI",
            "LT",
            "LU",
            "MO",
            "MK",
            "MG",
            "MW",
            "MY",
            "MV",
            "ML",
            "MT",
            "MH",
            "MR",
            "MU",
            "YT",
            "MX",
            "FM",
            "MD",
            "MC",
            "MN",
            "ME",
            "MS",
            "MA",
            "MZ",
            "MM",
            "NA",
            "NR",
            "NP",
            "NL",
            "AN",
            "NC",
            "NZ",
            "NI",
            "NE",
            "NG",
            "NU",
            "KP",
            "MP",
            "NO",
            "OM",
            "PK",
            "PW",
            "PS",
            "PA",
            "PG",
            "PY",
            "PE",
            "PH",
            "PN",
            "PL",
            "PT",
            "PR",
            "QA",
            "CG",
            "RE",
            "RO",
            "RU",
            "RW",
            "BL",
            "SH",
            "KN",
            "LC",
            "MF",
            "PM",
            "VC",
            "WS",
            "SM",
            "ST",
            "SA",
            "SN",
            "RS",
            "SC",
            "SL",
            "SG",
            "SX",
            "SK",
            "SI",
            "SB",
            "SO",
            "ZA",
            "KR",
            "SS",
            "ES",
            "LK",
            "SD",
            "SR",
            "SJ",
            "SZ",
            "SE",
            "CH",
            "SY",
            "TW",
            "TJ",
            "TZ",
            "TH",
            "TG",
            "TK",
            "TO",
            "TT",
            "TN",
            "TR",
            "TM",
            "TC",
            "TV",
            "VI",
            "UG",
            "UA",
            "AE",
            "GB",
            "US",
            "UY",
            "UZ",
            "VU",
            "VA",
            "VE",
            "VN",
            "WF",
            "EH",
            "YE",
            "ZM",
            "ZW"
        )
        val country = arrayOf(
            "Pais",
            "Afghanistan",
            "Albania",
            "Algeria",
            "American Samoa",
            "Andorra",
            "Angola",
            "Anguilla",
            "Antarctica",
            "Antigua and Barbuda",
            "Argentina",
            "Armenia",
            "Aruba",
            "Australia",
            "Austria",
            "Azerbaijan",
            "Bahamas",
            "Bahrain",
            "Bangladesh",
            "Barbados",
            "Belarus",
            "Belgium",
            "Belize",
            "Benin",
            "Bermuda",
            "Bhutan",
            "Bolivia",
            "Bosnia and Herzegovina",
            "Botswana",
            "Brazil",
            "British Indian Ocean Territory",
            "British Virgin Islands",
            "Brunei",
            "Bulgaria",
            "Burkina Faso",
            "Burundi",
            "Cambodia",
            "Cameroon",
            "Canada",
            "Cape Verde",
            "Cayman Islands",
            "Central African Republic",
            "Chad",
            "Chile",
            "China",
            "Christmas Island",
            "Cocos Islands",
            "Colombia",
            "Comoros",
            "Cook Islands",
            "Costa Rica",
            "Croatia",
            "Cuba",
            "Curacao",
            "Cyprus",
            "Czech Republic",
            "Democratic Republic of the Congo",
            "Denmark",
            "Djibouti",
            "Dominica",
            "Dominican Republic",
            "East Timor",
            "Ecuador",
            "Egypt",
            "El Salvador",
            "Equatorial Guinea",
            "Eritrea",
            "Estonia",
            "Ethiopia",
            "Falkland Islands",
            "Faroe Islands",
            "Fiji",
            "Finland",
            "France",
            "French Polynesia",
            "Gabon",
            "Gambia",
            "Georgia",
            "Germany",
            "Ghana",
            "Gibraltar",
            "Greece",
            "Greenland",
            "Grenada",
            "Guam",
            "Guatemala",
            "Guernsey",
            "Guinea",
            "Guinea Bissau",
            "Guyana",
            "Haiti",
            "Honduras",
            "Hong Kong",
            "Hungary",
            "Iceland",
            "India",
            "Indonesia",
            "Iran",
            "Iraq",
            "Ireland",
            "Isle of Man",
            "Israel",
            "Italy",
            "Ivory Coast",
            "Jamaica",
            "Japan",
            "Jersey",
            "Jordan",
            "Kazakhstan",
            "Kenya",
            "Kiribati",
            "Kosovo",
            "Kuwait",
            "Kyrgyzstan",
            "Laos",
            "Latvia",
            "Lebanon",
            "Lesotho",
            "Liberia",
            "Libya",
            "Liechtenstein",
            "Lithuania",
            "Luxembourg",
            "Macao",
            "Macedonia",
            "Madagascar",
            "Malawi",
            "Malaysia",
            "Maldives",
            "Mali",
            "Malta",
            "Marshall Islands",
            "Mauritania",
            "Mauritius",
            "Mayotte",
            "Mexico",
            "Micronesia",
            "Moldova",
            "Monaco",
            "Mongolia",
            "Montenegro",
            "Montserrat",
            "Morocco",
            "Mozambique",
            "Myanmar",
            "Namibia",
            "Nauru",
            "Nepal",
            "Netherlands",
            "Netherlands Antilles",
            "New Caledonia",
            "New Zealand",
            "Nicaragua",
            "Niger",
            "Nigeria",
            "Niue",
            "North Korea",
            "Northern Mariana Islands",
            "Norway",
            "Oman",
            "Pakistan",
            "Palau",
            "Palestine",
            "Panama",
            "Papua New Guinea",
            "Paraguay",
            "Peru",
            "Philippines",
            "Pitcairn",
            "Poland",
            "Portugal",
            "Puerto Rico",
            "Qatar",
            "Republic of the Congo",
            "Reunion",
            "Romania",
            "Russia",
            "Rwanda",
            "Saint Barthelemy",
            "Saint Helena",
            "Saint Kitts and Nevis",
            "Saint Lucia",
            "Saint Martin",
            "Saint Pierre and Miquelon",
            "Saint Vincent and the Grenadines",
            "Samoa",
            "San Marino",
            "Sao Tome and Principe",
            "Saudi Arabia",
            "Senegal",
            "Serbia",
            "Seychelles",
            "Sierra Leone",
            "Singapore",
            "Sint Maarten",
            "Slovakia",
            "Slovenia",
            "Solomon Islands",
            "Somalia",
            "South Africa",
            "South Korea",
            "South Sudan",
            "Spain",
            "Sri Lanka",
            "Sudan",
            "Suriname",
            "Svalbard and Jan Mayen",
            "Swaziland",
            "Sweden",
            "Switzerland",
            "Syria",
            "Taiwan",
            "Tajikistan",
            "Tanzania",
            "Thailand",
            "Togo",
            "Tokelau",
            "Tonga",
            "Trinidad and Tobago",
            "Tunisia",
            "Turkey",
            "Turkmenistan",
            "Turks and Caicos Islands",
            "Tuvalu",
            "U.S. Virgin Islands",
            "Uganda",
            "Ukraine",
            "United Arab Emirates",
            "United Kingdom",
            "United States",
            "Uruguay",
            "Uzbekistan",
            "Vanuatu",
            "Vatican",
            "Venezuela",
            "Vietnam",
            "Wallis and Futuna",
            "Western Sahara",
            "Yemen",
            "Zambia",
            "Zimbabwe"
        )

        val arrayList = ArrayList(listOf(*country))
        val adapter = ArrayAdapter(this, R.layout.style_spinner, arrayList)
        spinner.adapter = adapter

        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(adapterView: AdapterView<*>?, view: View, i: Int, l: Long) {
                if (i != 0) {
                    cc.setText(array[i])
                    phoneEdit.requestFocus()
                }
            }

            override fun onNothingSelected(adapterView: AdapterView<*>?) {}
        }

        phoneEdit.setOnKeyListener { view: View?, i: Int, keyEvent: KeyEvent? ->
            if (lastLength == 0) {
                if (i == KeyEvent.KEYCODE_DEL) {
                    cc.requestFocus()
                    cc.setSelection(cc.text.length)
                }
            }
            lastLength = phoneEdit.text.toString().length
            false
        }

        cc.setOnKeyListener { view: View?, i: Int, keyEvent: KeyEvent? ->
            if (listOf(*array).contains(cc.text.toString())) {
                val g =
                    listOf(*array).indexOf(cc.text.toString())
                spinner.setSelection(g)
                cc.requestFocus()
            }
            false
        }


        enterCode.setOnClickListener{
            this.login.visibility = View.GONE
            this.verify.visibility = View.VISIBLE
            this.wait.visibility = View.GONE
        }

        PushDownAnim.setPushDownAnimTo(enterBtn)
            .setDurationPush(100)
            .setScale(PushDownAnim.MODE_SCALE, 0.98f)
            .setOnClickListener{
                val num = "+" + cc.text.toString() + phoneEdit.text.toString()
                if (listOf(*array).contains(cc.text.toString())) {
                    val g = listOf(*array).indexOf(cc.text.toString())
                    onClickEnterPhone(num, countryCodes[g])
                }
            }

        PushDownAnim.setPushDownAnimTo(verifyBtn)
            .setDurationPush(100)
            .setScale(PushDownAnim.MODE_SCALE, 0.98f)
            .setOnClickListener{
                val code = codeInput.text.toString()
                verifyCode(code)
            }
        loginWithNumberPhone()*/
    }

    /*private fun loginWithNumberPhone(): Boolean{
        val sharedPreferences: SharedPreferences =
            getSharedPreferences("chat", Context.MODE_PRIVATE)
        val login = sharedPreferences.getBoolean("logged", false)

        if(login){
            return true
        }else{
            val timeRestant = sharedPreferences.getLong("verifyTimeOut", 0)
            if (Calendar.getInstance().timeInMillis >= timeRestant){
                this.login.visibility = View.VISIBLE
                this.verify.visibility = View.GONE
                this.wait.visibility = View.GONE
            }else{
                this.login.visibility = View.GONE
                this.verify.visibility = View.VISIBLE
                this.wait.visibility = View.GONE
            }
        }
        return false
    }

    private fun verifyCode(code: String) {
        if (storedVerificationId.isEmpty() || storedVerificationId == "") {
            val shared = getSharedPreferences("chat", Context.MODE_PRIVATE)
            storedVerificationId = shared.getString("VerificationId", "")!!
        }

        Log.d("cattoConsed", "$code $storedVerificationId")
        if(storedVerificationId.isNotEmpty() && storedVerificationId != ""){
            val credential = PhoneAuthProvider.getCredential(storedVerificationId, code)
            signInWithPhoneAuthCredential(credential)
        }
    }

    private fun onClickEnterPhone(phoneN : String, iso : String){
        val phoneUtil = PhoneNumberUtil.getInstance()
        try {
            val phoneNumber = phoneUtil.parse(phoneN, iso)
            if (phoneUtil.isValidNumber(phoneNumber)) {
                var phone = phoneUtil.format(
                    phoneNumber,
                    PhoneNumberUtil.PhoneNumberFormat.INTERNATIONAL
                )
                val re = Regex("[^0-9+]")
                phone = re.replace(phone, "")

                val phoneNational = phoneUtil.format(
                    phoneNumber,
                    PhoneNumberUtil.PhoneNumberFormat.NATIONAL
                )
                userPhoneNumber = re.replace(phoneNational, "")
                userCodePhoneNumber = iso

                wait.visibility = View.VISIBLE
                login.visibility = View.GONE

                startPhoneNumberVerification(phone)
            } else {
                Toast.makeText(this, "Number is not valid", Toast.LENGTH_SHORT).show()
            }
        } catch (e: NumberParseException) {
            e.printStackTrace()
        }
    }

    private fun startPhoneNumberVerification(phoneNumber: String) {
        val options = PhoneAuthOptions.newBuilder(auth)
            .setPhoneNumber(phoneNumber)
            .setTimeout(120L, TimeUnit.SECONDS)
            .setActivity(this)
            .setCallbacks(callbacks)
            .build()
        auth.setLanguageCode(Locale.getDefault().language)
        PhoneAuthProvider.verifyPhoneNumber(options)
    }

    private fun signInWithPhoneAuthCredential(credential: PhoneAuthCredential) {
        auth.signInWithCredential(credential)
            .addOnCompleteListener {
                if (it.isSuccessful) {
                    val user = it.result?.user
                    val phone = user?.phoneNumber
                    if (phone != null) {
                        loginUser(user!!.uid, phone)
                    }
                }
            }
    }

    private fun loginUser(userId: String, number : String) {
        val userBase = FirebaseDatabase.getInstance().reference.child("user").child(userId)
        FirebaseDatabase.getInstance().reference.orderByChild("user").equalTo(number)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val userHash = mapOf(
                        "name" to "",
                        "phone" to auth.currentUser?.phoneNumber,
                        "region" to userCodePhoneNumber
                    )

                    userBase.updateChildren(userHash)


                    val sharedPreferences: SharedPreferences =
                        getSharedPreferences("chat", Context.MODE_PRIVATE)
                    val editor = sharedPreferences.edit()
                    editor.putBoolean("logged", true)
                    editor.putLong("verifyTimeOut", 0)
                    editor.putString("regionCode", userCodePhoneNumber)
                    editor.apply()

                    val i = Intent(this@LoginActivity, MainActivity::class.java)

                    startActivity(i)
                    finish()
                }

                override fun onCancelled(error: DatabaseError) {

                }

            })
    }*/
}