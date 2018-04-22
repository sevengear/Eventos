package org.example.eventos;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.facebook.AccessToken;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.GraphRequest;
import com.facebook.GraphResponse;
import com.facebook.HttpMethod;
import com.facebook.Profile;
import com.facebook.login.LoginManager;
import com.facebook.login.LoginResult;
import com.facebook.login.widget.LoginButton;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.android.gms.appinvite.AppInvite;
import com.google.android.gms.appinvite.AppInviteInvitation;
import com.google.android.gms.appinvite.AppInviteInvitationResult;
import com.google.android.gms.appinvite.AppInviteReferral;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.remoteconfig.FirebaseRemoteConfig;
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings;
import com.google.firebase.storage.FirebaseStorage;
import com.crashlytics.android.Crashlytics;
import com.twitter.sdk.android.core.Callback;
import com.twitter.sdk.android.core.Result;
import com.twitter.sdk.android.core.Twitter;
import com.twitter.sdk.android.core.TwitterException;
import com.twitter.sdk.android.core.TwitterSession;
import com.twitter.sdk.android.core.identity.TwitterLoginButton;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;

import io.fabric.sdk.android.Fabric;

import static org.example.eventos.Comun.mostrarDialogo;
import static org.example.eventos.Comun.*;
import static org.example.eventos.EventosFirestore.EVENTOS;

public class ActividadPrincipal extends AppCompatActivity implements GoogleApiClient.OnConnectionFailedListener {

    private AdaptadorEventos adaptador;
    private GoogleApiClient mGoogleApiClient;
    private static final int REQUEST_INVITE = 0;
    private LoginButton loginButtonOfficial;
    private TwitterLoginButton botonLoginTwitter;
    private CallbackManager elCallbackManagerDeFacebook;

    // puntero a this para los callback
    private final Activity THIS = this;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Twitter.initialize(this);
        setContentView(R.layout.actividad_principal);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });

        //crearEventos();
        Query query = FirebaseFirestore.getInstance()
                .collection(EVENTOS)
                .limit(50);
        FirestoreRecyclerOptions<Evento> opciones = new FirestoreRecyclerOptions
                .Builder<Evento>().setQuery(query, Evento.class).build();
        adaptador = new AdaptadorEventos(opciones);
        final RecyclerView recyclerView = findViewById(R.id.reciclerViewEventos);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adaptador);

        final SharedPreferences preferencias = getApplicationContext().getSharedPreferences("Temas", Context.MODE_PRIVATE);
        if (preferencias.getBoolean("Inicializado", false) == false) {
            final SharedPreferences prefs = getApplicationContext().getSharedPreferences("Temas", Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = prefs.edit();
            editor.putBoolean("Inicializado", true);
            editor.commit();
            FirebaseMessaging.getInstance().subscribeToTopic("Todos");
        }

        adaptador.setOnItemClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                int position = recyclerView.getChildAdapterPosition(view);
                Evento currentItem = adaptador.getItem(position);
                String idEvento = adaptador.getSnapshots().getSnapshot(position).getId();
                Context context = getAppContext();
                Intent intent = new Intent(context, EventoDetalles.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                intent.putExtra("evento", idEvento);
                context.startActivity(intent);
            }
        });

        storage = FirebaseStorage.getInstance();
        storageRef = storage.getReferenceFromUrl("gs://eventos-82bb0.appspot.com/");

        String[] PERMISOS = {
                android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
                android.Manifest.permission.GET_ACCOUNTS,
                android.Manifest.permission.CAMERA};
        ActivityCompat.requestPermissions(this, PERMISOS, 1);

        mFirebaseAnalytics = FirebaseAnalytics.getInstance(this);

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(AppInvite.API)
                .enableAutoManage(this, this)
                .build();

        boolean autoLaunchDeepLink = true;
        AppInvite.AppInviteApi.getInvitation(mGoogleApiClient, this,
                autoLaunchDeepLink)
                .setResultCallback(
                        new ResultCallback<AppInviteInvitationResult>() {
                            @Override
                            public void onResult(AppInviteInvitationResult result) {
                                if (result.getStatus().isSuccess()) {
                                    Intent intent = result.getInvitationIntent();
                                    String deepLink = AppInviteReferral.getDeepLink(intent);
                                    String invitationId = AppInviteReferral
                                            .getInvitationId(intent);
                                    android.net.Uri url = Uri.parse(deepLink);
                                    String descuento = url.getQueryParameter("descuento");
                                    mostrarDialogo(getApplicationContext(),
                                            "Tienes un descuento del " + descuento
                                                    + "% gracias a la invitación: " + invitationId);
                                }
                            }
                        });

        mFirebaseRemoteConfig = FirebaseRemoteConfig.getInstance();
        FirebaseRemoteConfigSettings configSettings =
                new FirebaseRemoteConfigSettings
                        .Builder()
                        .setDeveloperModeEnabled(BuildConfig.DEBUG)
                        .build();
        mFirebaseRemoteConfig.setConfigSettings(configSettings);
        mFirebaseRemoteConfig.setDefaults(R.xml.remote_config_default);
        long cacheExpiration = 500;
        mFirebaseRemoteConfig.fetch(cacheExpiration)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        mFirebaseRemoteConfig.activateFetched();
                        getColorFondo();
                        getAcercaDe();
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception exception) {
                        colorFondo = mFirebaseRemoteConfig.getString("color_fondo");
                        acercaDe = mFirebaseRemoteConfig.getBoolean("acerca_de");
                    }
                });
        Fabric.with(this, new Crashlytics());

        loginButtonOfficial = findViewById(R.id.login_button);
        loginButtonOfficial.setPublishPermissions("publish_actions");

        //FacebookSdk.sdkInitialize(this);
        this.elCallbackManagerDeFacebook = CallbackManager.Factory.create();

        LoginManager.getInstance().registerCallback(this.elCallbackManagerDeFacebook, new FacebookCallback<LoginResult>() {
            @Override
            public void onSuccess(LoginResult loginResult) {
                Toast.makeText(THIS, "Login onSuccess()", Toast.LENGTH_LONG).show();
                actualizarVentanita();
            }

            @Override
            public void onCancel() {
                Toast.makeText(THIS, "Login onCancel()", Toast.LENGTH_LONG).show();
                actualizarVentanita();
            }

            @Override
            public void onError(FacebookException error) {
                Toast.makeText(THIS, "Login onError(): " + error.getMessage(), Toast.LENGTH_LONG).show();
                actualizarVentanita();
            }
        });

        botonLoginTwitter = findViewById(R.id.twitter_login_button);
        botonLoginTwitter.setCallback(new Callback<TwitterSession>() {
            @Override
            public void success(Result<TwitterSession> result) {
                Toast.makeText(THIS, "Autenticado en twitter: " + result.data.getUserName(), Toast.LENGTH_LONG).show();
            }

            @Override
            public void failure(TwitterException e) {
                Toast.makeText(THIS, "Fallo en autentificación: " +
                        e.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_actividad_principal, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_temas) {
            Bundle bundle = new Bundle();
            bundle.putString(FirebaseAnalytics.Param.ITEM_NAME, "suscripciones");
            mFirebaseAnalytics.logEvent("menus", bundle);
            Intent intent = new Intent(getBaseContext(), Temas.class);
            startActivity(intent);
            return true;
        }
        if (id == R.id.action_invitar) {
            invitar();
        }
        if (id == R.id.action_error) {
            Crashlytics.getInstance().crash();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onStart() {
        super.onStart();
        adaptador.startListening();
        current = this;
    }

    @Override
    public void onStop() {
        super.onStop();
        adaptador.stopListening();
    }

    private static ActividadPrincipal current;

    public static ActividadPrincipal getCurrentContext() {
        return current;
    }

    @Override
    protected void onResume() {
        super.onResume();
        Bundle extras = getIntent().getExtras();
        if (extras != null && extras.keySet().size() > 4) {
            String evento = "";
            evento = "Evento: " + extras.getString("evento") + "\n";
            evento = evento + "Día: " + extras.getString("dia") + "\n";
            evento = evento + "Ciudad: " + extras.getString("ciudad") + "\n";
            evento = evento + "Comentario: " + extras.getString("comentario");
            mostrarDialogo(getApplicationContext(), evento);
            for (String key : extras.keySet()) {
                getIntent().removeExtra(key);
            }
            extras = null;
        }
    }

    public static Context getAppContext() {
        return ActividadPrincipal.getCurrentContext();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case 1: {
                if (!(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    Toast.makeText(ActividadPrincipal.this, "Has denegado algún permiso de la aplicación.", Toast.LENGTH_SHORT).show();
                }
                return;
            }
        }
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Toast.makeText(this, "Error al enviar la invitación", Toast.LENGTH_LONG);
    }

    private void invitar() {
        Intent intent = new AppInviteInvitation.IntentBuilder(getString(
                R.string.invitation_title))
                .setMessage(getString(R.string.invitation_message))
                .setDeepLink(Uri.parse(getString(R.string.invitation_deep_link)))
                .setCustomImage(Uri.parse(getString(R.string.invitation_custom_image)))
                .setCallToActionText(getString(R.string.invitation_cta))
                .build();
        startActivityForResult(intent, REQUEST_INVITE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode,
                                    Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_INVITE) {
            if (resultCode == RESULT_OK) {
                String[] ids = AppInviteInvitation.getInvitationIds(resultCode, data);
            } else {
                Toast.makeText(this, "Error al enviar la invitación",
                        Toast.LENGTH_LONG);
            }
        } else if (requestCode == 140) {
            botonLoginTwitter.onActivityResult(requestCode, resultCode, data);
        } else {
            this.elCallbackManagerDeFacebook.onActivityResult(requestCode, resultCode, data);

        }
    }

    private void getColorFondo() {
        colorFondo = mFirebaseRemoteConfig.getString("color_fondo");
    }

    private void getAcercaDe() {
        acercaDe = mFirebaseRemoteConfig.getBoolean("acerca_de");
    }

    private void actualizarVentanita() {
        Log.d("cuandrav.actualizarVent", "empiezo");
        // obtengo el access token para ver si hay sesión
        AccessToken accessToken = this.obtenerAccessToken();
        if (accessToken == null) {
            Log.d("cuandrav.actualizarVent", "no hay sesion, deshabilito");
            return;
        }
        // sí hay sesión
        Log.d("cuandrav.actualizarVent", "hay sesion habilito");
        // averiguo los datos básicos del usuario acreditado
        Profile profile = Profile.getCurrentProfile();
        // otra forma de averiguar los datos básicos:
        // hago una petición con "graph api" para obtener datos del
        // usuario acreditado
        this.obtenerPublicProfileConRequest_async(
                // como es asíncrono he de dar un callback
                new GraphRequest.GraphJSONObjectCallback() {
                    @Override
                    public void onCompleted(JSONObject datosJSON, GraphResponse response) {
                        // muestro los datos
                        String nombre = "nombre desconocido";
                        try {
                            nombre = datosJSON.getString("name");
                        } catch (org.json.JSONException ex) {
                            Log.d("cuandrav.actualizarVent", "callback de obtenerPublicProfileConRequest_async: excepcion: "
                                    + ex.getMessage());
                        } catch (NullPointerException ex) {
                            Log.d("cuandrav.actualizarVent", "callback de obtenerPublicProfileConRequest_async: excepcion: "
                                    + ex.getMessage());
                        }
                    }
                });
    }

    private void obtenerPublicProfileConRequest_async(GraphRequest.GraphJSONObjectCallback callback) {
        if (!this.hayRed()) {
            Toast.makeText(this, "¿No hay red?", Toast.LENGTH_LONG).show();
        }
        // obtengo access token y compruebo que hay sesión
        AccessToken accessToken = obtenerAccessToken();
        if (accessToken == null) {
            Toast.makeText(THIS, "no hay sesión con Facebook", Toast.LENGTH_LONG).show();
            return;
        }
        // monto la petición: /me
        GraphRequest request = GraphRequest.newMeRequest(accessToken, callback);
        Bundle params = new Bundle();
        params.putString("fields", "id, name");
        request.setParameters(params);
        // la ejecuto (asíncronamente)
        request.executeAsync();
    }

    private boolean hayRed() {
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager
                .getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    private AccessToken obtenerAccessToken() {
        return AccessToken.getCurrentAccessToken();
    }

    private boolean sePuedePublicar() {
        // compruebo la red
        if (!this.hayRed()) {
            Toast.makeText(this, "¿no hay red? No puedo publicar", Toast.LENGTH_LONG).show();
            return false;
        }
        // compruebo permisos
        if (!this.tengoPermisoParaPublicar()) {
            Toast.makeText(this, "¿no tengo permisos para publicar? Los pido.", Toast.LENGTH_LONG).show();
            LoginManager.getInstance().logInWithPublishPermissions(this, Arrays.asList("publish_actions"));
            return false;
        }
        return true;
    }

    private boolean tengoPermisoParaPublicar() {
        AccessToken accessToken = this.obtenerAccessToken();
        return accessToken != null && accessToken.getPermissions().contains("publish_actions");
    }

    public void enviarTextoAFacebook_async(final String textoQueEnviar) {
        // si no se puede publicar no hago nada
        if (!sePuedePublicar()) {
            return;
        }
        // hago la petición a través del API Graph
        Bundle params = new Bundle();
        params.putString("message", textoQueEnviar);
        GraphRequest request = new GraphRequest(
                AccessToken.getCurrentAccessToken(),
                "/me/feed",
                params,
                HttpMethod.POST,
                new GraphRequest.Callback() {
                    public void onCompleted(GraphResponse response) {
                        Toast.makeText(THIS, "Publicación realizada: " + textoQueEnviar, Toast.LENGTH_LONG).show();
                    }
                }
        );
        request.executeAsync();
    }

    public void enviarFotoAFacebook_async(Bitmap image, String comentario) {
        Log.d("cuandrav.envFotoFBasync", "llamado");
        if (image == null) {
            Toast.makeText(this, "Enviar foto: la imagen está vacía.", Toast.LENGTH_LONG).show();
            Log.d("cuandrav.envFotoFBasync", "acabo porque la imagen es null");
            return;
        }
        // si no se puede publicar no hago nada
        if (!sePuedePublicar()) {
            return;
        }
        // convierto el bitmap a array de bytes
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        image.compress(Bitmap.CompressFormat.PNG, 100, stream);
        //image.recycle ();
        final byte[] byteArray = stream.toByteArray();
        try {
            stream.close();
        } catch (IOException e) {
        }
        // hago la petición a traves del Graph API
        Bundle params = new Bundle();
        params.putByteArray("source", byteArray); // bytes de la imagen
        params.putString("caption", comentario); // comentario
        // si se quisiera publicar una imagen de internet: // params.putString("url", "{image-url}");
        GraphRequest request = new GraphRequest(
                AccessToken.getCurrentAccessToken(),
                "/me/photos",
                params,
                HttpMethod.POST,
                new GraphRequest.Callback() {
                    public void onCompleted(GraphResponse response) {
                        Toast.makeText(THIS, "" + byteArray.length + " Foto enviada: " + response.toString(), Toast.LENGTH_LONG).show();
                        //textoConElMensaje.setText(response.toString());
                    }
                }
        );
        request.executeAsync();
    }

    public void boton_Login_pulsado(View quien) {
        // compruebo la red
        if (!this.hayRed()) {
            Toast.makeText(this, "¿No hay red? No puedo abrir sesión",
                    Toast.LENGTH_LONG).show();
        }
        // login
        LoginManager.getInstance().logInWithPublishPermissions(this, Arrays.asList("publish_actions"));
        // actualizar
        this.actualizarVentanita();
    }

    public void boton_Logout_pulsado(View quien) {
        // compruebo la red
        if (!this.hayRed()) {
            Toast.makeText(this, "¿No hay red? No puedo cerrar sesión",
                    Toast.LENGTH_LONG).show();
        }
        // logout
        LoginManager.getInstance().logOut();
        // actualizar
        this.actualizarVentanita();
    }
}
