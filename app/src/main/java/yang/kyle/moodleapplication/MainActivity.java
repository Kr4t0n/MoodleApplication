package yang.kyle.moodleapplication;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.nio.Buffer;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

public class MainActivity extends Activity implements View.OnClickListener {
    EditText txt_UserName, txt_UserPW;
    Button btn_Login;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        btn_Login = (Button)findViewById(R.id.btn_Login);
        txt_UserName = (EditText)findViewById(R.id.txt_UserName);
        txt_UserPW = (EditText)findViewById(R.id.txt_UserPW);

        // Register the Login button to click listener
        // Whenever the button is clicked, onClick is called
        btn_Login.setOnClickListener(this);

        doTrustToCertificate();
        CookieHandler.setDefault(new CookieManager());
    }

    public String ReadBufferedHTML(BufferedReader reader, char [] htmlBuffer, int bufSz) throws java.io.IOException {
        htmlBuffer[0] = '\0';
        int offset = 0;
        do {
            int cnt = reader.read(htmlBuffer, offset, bufSz - offset);
            if (cnt > 0) {
                offset += cnt;
            }
            else {
                break;
            }
        }while (true);
        return new String(htmlBuffer);
    }

    public String getMoodleFirstPage(String userName, String userPW) {
        HttpsURLConnection conn_portal = null;
        URLConnection conn_moodle = null;

        final int HTML_BUFFER_SIZE = 2 * 1024 * 1024;
        char htmlBuffer[] = new char[HTML_BUFFER_SIZE];

        final int HTTPCONNECTION_TYPE = 0;
        final int HTTPSCONNECTION_TYPE = 1;
        int moodle_conn_type = HTTPCONNECTION_TYPE;

        try {
            /////////////////////////////////// HKU portal //////////////////////////////////////
            URL url_portal = new
                    URL("https://hkuportal.hku.hk/cas/login?service=http://moodle.hku.hk/login/index.php?authCAS=CAS&username="
                    + userName + "&password=" + userPW);
            conn_portal = (HttpsURLConnection) url_portal.openConnection();

            BufferedReader reader_portal = new BufferedReader(new InputStreamReader(conn_portal.getInputStream()));
            String HTMLSource = ReadBufferedHTML(reader_portal, htmlBuffer, HTML_BUFFER_SIZE);
            int ticketIDStartPosition = HTMLSource.indexOf("ticket=") + 7;
            String ticketID = HTMLSource.substring(ticketIDStartPosition, HTMLSource.indexOf("\";", ticketIDStartPosition));
            reader_portal.close();
            /////////////////////////////////// HKU portal //////////////////////////////////////

            /////////////////////////////////// Moodle //////////////////////////////////////
            URL url_moodle = new URL("http://moodle.hku.hk/login/index.php?authCAS=CAS&ticket=" + ticketID);
            conn_moodle =  url_moodle.openConnection();
            ((HttpURLConnection)conn_moodle).setInstanceFollowRedirects(true);

            BufferedReader reader_moodle = new BufferedReader(new InputStreamReader(conn_moodle.getInputStream()));

            /// handling redirects to HTTPS protocol
            while(true) {
                String redirect_moodle = conn_moodle.getHeaderField("Location");
                if (redirect_moodle != null) {
                    URL new_url_moodle = new URL(url_moodle, redirect_moodle);
                    if(moodle_conn_type == HTTPCONNECTION_TYPE) {
                        ((HttpURLConnection) conn_moodle).disconnect();
                    } else {
                        ((HttpsURLConnection) conn_moodle).disconnect();
                    }
                    conn_moodle =  new_url_moodle.openConnection();
                    if(new_url_moodle.getProtocol().equals("http")) {
                        moodle_conn_type = HTTPCONNECTION_TYPE;
                        ((HttpURLConnection)conn_moodle).setInstanceFollowRedirects(true);
                    } else {
                        moodle_conn_type = HTTPSCONNECTION_TYPE;
                        ((HttpsURLConnection)conn_moodle).setInstanceFollowRedirects(true);
                    }

                    url_moodle = new_url_moodle;

                    //String cookie = conn_moodle.getHeaderField("Set-Cookie");
                    //if (cookie != null) {
                    //    conn_moodle2.setRequestProperty("Cookie", cookie);
                    //}
                    reader_moodle = new BufferedReader(new InputStreamReader(conn_moodle.getInputStream()));
                } else {
                    break;
                }
            }

            HTMLSource = ReadBufferedHTML(reader_moodle, htmlBuffer, HTML_BUFFER_SIZE);
            reader_moodle.close();
            return HTMLSource;
            /////////////////////////////////// Moodle //////////////////////////////////////
        }
        catch (Exception e) {
            return "Fail to login";
        }
        finally {
            // When HttpClient instance is no longer needed,
            // shut down the connection manager to ensure
            // immediate deallocation of all system resources
            if (conn_portal != null) {
                if (moodle_conn_type == HTTPCONNECTION_TYPE) {
                    ((HttpURLConnection)conn_moodle).disconnect();
                }
                else {
                    ((HttpsURLConnection)conn_moodle).disconnect();
                }
            }
        }
    }

    public void doTrustToCertificate() {
        TrustManager[] trustAllCerts = new TrustManager[] {
                new X509TrustManager() {
                    @Override
                    public void checkClientTrusted(X509Certificate[] x509Certificates, String s) throws CertificateException {

                    }

                    @Override
                    public void checkServerTrusted(X509Certificate[] x509Certificates, String s) throws CertificateException {

                    }

                    @Override
                    public X509Certificate[] getAcceptedIssuers() {
                        return null;
                    }
                }
        };

        try {
            // install the all-trusting trust manager
            SSLContext sc = SSLContext.getInstance("SSL");
            sc.init(null, trustAllCerts, new java.security.SecureRandom());
            HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    protected void alert(String title, String mymessage) {
        new AlertDialog.Builder(this)
                .setMessage(mymessage)
                .setTitle(title)
                .setCancelable(true)
                .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {}
                })
                .show();
    }

    public void parse_HTML_Source_and_Switch_Activity(String HTMLsource) {
        Pattern p_coursename = Pattern.compile("<h3 class=\"coursename\".*?>.*?>(.*?)</a>");
        Matcher m_course = p_coursename.matcher(HTMLsource);
        Pattern p_teachercandidates = Pattern.compile("<div class=\"teachers\">Teacher: <.*?>(.*?)</a>");
        Matcher m_teachercandidates = p_teachercandidates.matcher(HTMLsource);

        ArrayList<String> cname = new ArrayList<String>();
        ArrayList<String> cteachers = new ArrayList<String>();
        ArrayList<String> cteachersfinal = new ArrayList<String>();
        ArrayList<Integer> cnamePos = new ArrayList<Integer>();
        ArrayList<Integer> cteachersPos = new ArrayList<Integer>();
        ArrayList<Integer> cteachersIdx = new ArrayList<Integer>();

        while (m_course.find()) {
            String course_name = m_course.group(1);
            Integer pos = m_course.start();
            boolean flag = true;
            for (String sss:cname) {
                if (sss.equals(course_name)) {
                    flag = false;
                }
            }
            if (flag) {
                cname.add(course_name);
                cnamePos.add(pos);
            }
        }

        while (m_teachercandidates.find()) {
            String string_teachername = m_teachercandidates.group(1);
            cteachers.add(string_teachername);
            Integer pos = m_teachercandidates.start();
            cteachersPos.add(pos);
        }

        Intent intent = new Intent(getBaseContext(), CourseListActivity.class);

        int cIdx = 0;
        for (int i = 0; i < cteachersPos.size(); ) {
            int cpos0 = -1, cpos1 = -1;
            int tpos = cteachersPos.get(i);
            if (cIdx < cnamePos.size()) {
                cpos0 = cnamePos.get(cIdx);
            }
            if (cIdx + 1 < cnamePos.size()) {
                cpos1 = cnamePos.get(cIdx + 1);
            }
            if (cpos0 < 0 || tpos < cpos0) { // a course with 2 teachers!? Assume the teacher belongs to the privous course
                cteachersIdx.add(cIdx - 1);
                i++;
            }
            else if (cpos1 < 0 || (cpos0 < tpos && cpos1 > tpos)) {
                cteachersIdx.add(cIdx);
                i++;
                cIdx++;
            }
            else { // tpos > cpos1 ====> teacher belongs to next classes
                cIdx++;
            }
        }

        for (int i = 0; i < cname.size(); i++) {
            String tname = "";
            for (int j = 0; j < cteachersIdx.size(); j++) {
                int cidx = cteachersIdx.get(j);
                if (cidx == i) {
                    tname += cteachers.get(j);
                }
            }
            cteachersfinal.add(tname);
        }

        intent.putStringArrayListExtra("CourseName", cname);
        intent.putStringArrayListExtra("Teachers", cteachersfinal);
        startActivity(intent);
    }

    public void connect(final String userName, final String userPW) {
        final ProgressDialog pdialog = new ProgressDialog(this);

        pdialog.setCancelable(false);
        pdialog.setMessage("Logging in ...");
        pdialog.show();

        AsyncTask<String, Void, String> task = new AsyncTask<String, Void, String>() {
            boolean success;
            String moodlePageContect;

            @Override
            protected String doInBackground(String... arg0) {
                // TODO Auto-generated method stub
                success = true;
                moodlePageContect = getMoodleFirstPage(userName, userPW);

                if (moodlePageContect.equals("Fail to login")) {
                    success = false;
                }

                return null;
            }

            @Override
            protected void onPostExecute(String result) {
                if (success) {
                    parse_HTML_Source_and_Switch_Activity(moodlePageContect);
                }
                else {
                    alert("Error", "Fail to login");
                }
                pdialog.hide();
            }
        }.execute("");
    }

    @Override
    public void onClick(View v) {
        // TODO Auto-generated method stub
        // btn_Login is the ID of Login button defined in the layout
        if(v.getId() == R.id.btn_Login) {
            String uname = txt_UserName.getText().toString();
            String upassword = txt_UserPW.getText().toString();

            connect(uname, upassword);
        }
    }
}