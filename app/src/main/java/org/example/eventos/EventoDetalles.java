package org.example.eventos;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.facebook.AccessToken;
import com.facebook.GraphRequest;
import com.facebook.GraphResponse;
import com.facebook.HttpMethod;
import com.facebook.login.LoginManager;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.storage.FileDownloadTask;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnPausedListener;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.twitter.sdk.android.core.Callback;
import com.twitter.sdk.android.core.Result;
import com.twitter.sdk.android.core.Twitter;
import com.twitter.sdk.android.core.TwitterCore;
import com.twitter.sdk.android.core.TwitterException;
import com.twitter.sdk.android.core.TwitterSession;
import com.twitter.sdk.android.core.models.Media;
import com.twitter.sdk.android.core.models.Tweet;
import com.twitter.sdk.android.core.services.MediaService;
import com.twitter.sdk.android.core.services.StatusesService;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import okhttp3.MediaType;
import retrofit.mime.TypedFile;
import retrofit2.Call;

import static org.example.eventos.Comun.acercaDe;
import static org.example.eventos.Comun.getStorageReference;
import static org.example.eventos.Comun.mFirebaseAnalytics;
import static org.example.eventos.Comun.mostrarDialogo;

/**
 * Created by Miguel Á. Núñez on 22/03/2018.
 */

public class EventoDetalles extends AppCompatActivity {

    final int SOLICITUD_FOTOGRAFIAS_DRIVE = 102;
    private final Activity THIS = this;

    TextView txtEvento, txtFecha, txtCiudad;
    ImageView imgImagen;
    Button txtFbk, imgFbk, txtTwt, imgTwt;
    EditText txtInput;
    String evento;
    CollectionReference registros;
    final int SOLICITUD_SUBIR_PUTDATA = 0;
    final int SOLICITUD_SUBIR_PUTSTREAM = 1;
    final int SOLICITUD_SUBIR_PUTFILE = 2;
    final int SOLICITUD_SELECCION_STREAM = 100;
    final int SOLICITUD_SELECCION_PUTFILE = 101;
    private ProgressDialog progresoSubida;
    Boolean subiendoDatos = false;
    static UploadTask uploadTask = null;
    StorageReference imagenRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.evento_detalles);
        txtEvento = findViewById(R.id.txtEvento);
        txtFecha = findViewById(R.id.txtFecha);
        txtCiudad = findViewById(R.id.txtCiudad);
        imgImagen = findViewById(R.id.imgImagen);
        txtFbk = findViewById(R.id.publishTextGraph);
        imgFbk = findViewById(R.id.publishImageGraph);
        txtTwt = findViewById(R.id.publishTextTwitter);
        imgTwt = findViewById(R.id.publishImageTwitter);
        txtInput = findViewById(R.id.textoFacebook);
        Bundle extras = getIntent().getExtras();
        evento = extras.getString("evento");
        if (evento == null) {
            android.net.Uri url = getIntent().getData();
            evento = url.getQueryParameter("evento");
        }
        registros = FirebaseFirestore.getInstance().collection("eventos");
        registros.document(evento).get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                if (task.isSuccessful()) {
                    txtEvento.setText(task.getResult().get("evento").toString());
                    txtCiudad.setText(task.getResult().get("ciudad").toString());
                    txtFecha.setText(task.getResult().get("fecha").toString());
                    new DownloadImageTask(imgImagen).execute(task.getResult().get("imagen").toString());
                }
            }
        });

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        txtFbk.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                boton_enviarTextoAFB_pulsado(view);
            }
        });
        imgFbk.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Bitmap bmp = ((BitmapDrawable) imgImagen.getDrawable()).getBitmap();
                enviarFotoAFacebook_async(bmp, txtInput.getText().toString());
            }
        });
        txtTwt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                publishtweet();
            }
        });
        imgTwt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                publishTweetWithImage();
            }
        });
    }

    private class DownloadImageTask extends AsyncTask<String, Void, Bitmap> {
        ImageView bmImage;

        public DownloadImageTask(ImageView bmImage) {
            this.bmImage = bmImage;
        }

        protected Bitmap doInBackground(String... urls) {
            String urldisplay = urls[0];
            Bitmap mImagen = null;
            try {
                InputStream in = new java.net.URL(urldisplay).openStream();
                mImagen = BitmapFactory.decodeStream(in);
            } catch (Exception e) {
                e.printStackTrace();
            }
            return mImagen;
        }

        protected void onPostExecute(Bitmap result) {
            bmImage.setImageBitmap(result);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_detalles, menu);
        if (!acercaDe) {
            menu.removeItem(R.id.action_acercaDe);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        View vista = (View) findViewById(android.R.id.content);
        Bundle bundle = new Bundle();
        int id = item.getItemId();
        switch (id) {
            case R.id.action_putData:
                bundle.putString(FirebaseAnalytics.Param.ITEM_NAME, "subir_imagen");
                mFirebaseAnalytics.logEvent("menus", bundle);
                subirAFirebaseStorage(SOLICITUD_SUBIR_PUTDATA, null);
                break;
            case R.id.action_streamData:
                bundle.putString(FirebaseAnalytics.Param.ITEM_NAME, "subir_stream");
                mFirebaseAnalytics.logEvent("menus", bundle);
                seleccionarFotografiaDispositivo(vista, SOLICITUD_SELECCION_STREAM);
                break;
            case R.id.action_putFile:
                bundle.putString(FirebaseAnalytics.Param.ITEM_NAME, "subir_fichero");
                mFirebaseAnalytics.logEvent("menus", bundle);
                seleccionarFotografiaDispositivo(vista, SOLICITUD_SELECCION_PUTFILE);
                break;
            case R.id.action_getFile:
                bundle.putString(FirebaseAnalytics.Param.ITEM_NAME, "descargar_fichero");
                mFirebaseAnalytics.logEvent("menus", bundle);
                descargarDeFirebaseStorage(evento);
                break;
            case R.id.action_fotografiasDrive:
                bundle.putString(FirebaseAnalytics.Param.ITEM_NAME, "fotografias_drive");
                mFirebaseAnalytics.logEvent("menus", bundle);
                Intent intent = new Intent(getBaseContext(), FotografiasDrive.class);
                intent.putExtra("evento", evento);
                startActivity(intent);
                break;
            case R.id.action_acercaDe:
                bundle.putString(FirebaseAnalytics.Param.ITEM_NAME, "acerca_de");
                mFirebaseAnalytics.logEvent("menus", bundle);
                Intent intentWeb = new Intent(getBaseContext(), EventosWeb.class);
                intentWeb.putExtra("evento", evento);
                startActivity(intentWeb);
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    public void seleccionarFotografiaDispositivo(View v, Integer solicitud) {
        Intent seleccionFotografiaIntent = new Intent(Intent.ACTION_PICK);
        seleccionFotografiaIntent.setType("image/*");
        startActivityForResult(seleccionFotografiaIntent, solicitud);
    }

    @Override
    protected void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
        Uri ficheroSeleccionado;
        Cursor cursor;
        String rutaImagen;
        if (resultCode == Activity.RESULT_OK) {
            switch (requestCode) {
                case SOLICITUD_SELECCION_STREAM:
                    ficheroSeleccionado = data.getData();
                    String[] proyeccionStream = {MediaStore.Images.Media.DATA};
                    cursor = getContentResolver().query(ficheroSeleccionado, proyeccionStream, null, null, null);
                    cursor.moveToFirst();
                    rutaImagen = cursor.getString(cursor.getColumnIndex(proyeccionStream[0]));
                    cursor.close();
                    subirAFirebaseStorage(SOLICITUD_SUBIR_PUTSTREAM, rutaImagen);
                    break;
                case SOLICITUD_SELECCION_PUTFILE:
                    ficheroSeleccionado = data.getData();
                    String[] proyeccionFile = {MediaStore.Images.Media.DATA};
                    cursor = getContentResolver().query(ficheroSeleccionado, proyeccionFile, null, null, null);
                    cursor.moveToFirst();
                    rutaImagen = cursor.getString(cursor.getColumnIndex(proyeccionFile[0]));
                    cursor.close();
                    subirAFirebaseStorage(SOLICITUD_SUBIR_PUTFILE, rutaImagen);
                    break;
            }
        }
    }

    public void subirAFirebaseStorage(Integer opcion, String ficheroDispositivo) {
        final ProgressDialog progresoSubida = new ProgressDialog(EventoDetalles.this);
        progresoSubida.setTitle("Subiendo...");
        progresoSubida.setMessage("Espere...");
        progresoSubida.setCancelable(true);
        progresoSubida.setCanceledOnTouchOutside(false);
        progresoSubida.setButton(DialogInterface.BUTTON_NEGATIVE, "Cancelar", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                uploadTask.cancel();
            }
        });
        String fichero = evento;
        imagenRef = getStorageReference().child(fichero);
        try {
            switch (opcion) {
                case SOLICITUD_SUBIR_PUTDATA:
                    imgImagen.setDrawingCacheEnabled(true);
                    imgImagen.buildDrawingCache();
                    Bitmap bitmap = imgImagen.getDrawingCache();
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);
                    byte[] data = baos.toByteArray();
                    uploadTask = imagenRef.putBytes(data);
                    break;
                case SOLICITUD_SUBIR_PUTSTREAM:
                    InputStream stream = new FileInputStream(new File(ficheroDispositivo));
                    uploadTask = imagenRef.putStream(stream);
                    break;
                case SOLICITUD_SUBIR_PUTFILE:
                    Uri file = Uri.fromFile(new File(ficheroDispositivo));
                    uploadTask = imagenRef.putFile(file);
                    break;
            }

            uploadTask.addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception exception) {
                    subiendoDatos = false;
                    mostrarDialogo(getApplicationContext(), "Ha ocurrido un error al subir la imagen o el usuario ha cancelado la subida.");
                }
            }).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                @Override
                public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                    Map<String, Object> datos = new HashMap<>();
                    datos.put("imagen", taskSnapshot.getDownloadUrl().toString());
                    FirebaseFirestore.getInstance().collection("eventos").document(evento).set(datos, SetOptions.merge());
                    new DownloadImageTask((ImageView) imgImagen).execute(taskSnapshot.getDownloadUrl().toString());
                    progresoSubida.dismiss();
                    subiendoDatos = false;
                    mostrarDialogo(getApplicationContext(), "Imagen subida correctamente.");
                }
            }).addOnProgressListener(new OnProgressListener<UploadTask.TaskSnapshot>() {
                @Override
                public void onProgress(UploadTask.TaskSnapshot taskSnapshot) {
                    if (!subiendoDatos) {
                        progresoSubida.show();
                        subiendoDatos = true;
                    } else {
                        if (taskSnapshot.getTotalByteCount() > 0)
                            progresoSubida.setMessage("Espere... "
                                    + String.valueOf(100 * taskSnapshot.getBytesTransferred()
                                    / taskSnapshot.getTotalByteCount()) + "%");
                    }
                }
            }).addOnPausedListener(new OnPausedListener<UploadTask.TaskSnapshot>() {
                @Override
                public void onPaused(UploadTask.TaskSnapshot taskSnapshot) {
                    subiendoDatos = false;
                    mostrarDialogo(getApplicationContext(), "La subida ha sido pausada.");
                }
            });

        } catch (IOException e) {
            mostrarDialogo(getApplicationContext(), e.toString());
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (imagenRef != null) {
            outState.putString("EXTRA_STORAGE_REFERENCE_KEY", imagenRef.toString());
        }
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        final String stringRef = savedInstanceState.getString("EXTRA_STORAGE_REFERENCE_KEY");
        if (stringRef == null) {
            return;
        }
        imagenRef = FirebaseStorage.getInstance().getReferenceFromUrl(stringRef);
        List<UploadTask> tasks = imagenRef.getActiveUploadTasks();
        for (UploadTask task : tasks) {
            task.addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception exception) {
                    upload_error(exception);
                }
            }).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                @Override
                public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                    upload_exito(taskSnapshot);
                }
            }).addOnProgressListener(new OnProgressListener<UploadTask.TaskSnapshot>() {
                @Override
                public void onProgress(UploadTask.TaskSnapshot taskSnapshot) {
                    upload_progreso(taskSnapshot);
                }
            }).addOnPausedListener(new OnPausedListener<UploadTask.TaskSnapshot>() {
                @Override
                public void onPaused(UploadTask.TaskSnapshot taskSnapshot) {
                    upload_pausa(taskSnapshot);
                }
            });
        }
    }

    private void upload_error(Exception exception) {
        subiendoDatos = false;
        mostrarDialogo(getApplicationContext(), "Ha ocurrido un error al subir la imagen o el usuario ha cancelado la subida.");
    }

    private void upload_exito(UploadTask.TaskSnapshot taskSnapshot) {
        Map<String, Object> datos = new HashMap<>();
        datos.put("imagen", taskSnapshot.getDownloadUrl().toString());
        FirebaseFirestore.getInstance().collection("eventos").document(evento).set(datos);
        new DownloadImageTask((ImageView) imgImagen).execute(taskSnapshot.getDownloadUrl().toString());
        progresoSubida.dismiss();
        subiendoDatos = false;
        mostrarDialogo(getApplicationContext(), "Imagen subida correctamente.");
    }

    private void upload_progreso(UploadTask.TaskSnapshot taskSnapshot) {
        if (!subiendoDatos) {
            progresoSubida = new ProgressDialog(EventoDetalles.this);
            progresoSubida.setTitle("Subiendo...");
            progresoSubida.setMessage("Espere...");
            progresoSubida.setCancelable(true);
            progresoSubida.setCanceledOnTouchOutside(false);
            progresoSubida.setButton(DialogInterface.BUTTON_NEGATIVE, "Cancelar", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    uploadTask.cancel();
                }
            });
            progresoSubida.show();
            subiendoDatos = true;
        } else {
            if (taskSnapshot.getTotalByteCount() > 0)
                progresoSubida.setMessage("Espere... " + String.valueOf(100 * taskSnapshot.getBytesTransferred() / taskSnapshot.getTotalByteCount()) + "%");
        }
    }

    private void upload_pausa(UploadTask.TaskSnapshot taskSnapshot) {
        subiendoDatos = false;
        mostrarDialogo(getApplicationContext(), "La subida ha sido pausada.");
    }

    public void descargarDeFirebaseStorage(String fichero) {
        StorageReference referenciaFichero = getStorageReference().child(fichero);
        File rootPath = new File(Environment.getExternalStorageDirectory(), "Eventos");
        if (!rootPath.exists()) {
            rootPath.mkdirs();
        }
        final File localFile = new File(rootPath, evento + ".jpg");
        referenciaFichero.getFile(localFile).addOnSuccessListener(new OnSuccessListener<FileDownloadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(FileDownloadTask.TaskSnapshot taskSnapshot) {
                mostrarDialogo(getApplicationContext(), "Fichero descargado con éxito: " + localFile.toString());
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception exception) {
                mostrarDialogo(getApplicationContext(), "Error al descargar el fichero.");
            }
        });
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
                        txtInput.setText(response.toString());
                    }
                }
        );
        request.executeAsync();
    }

    public void boton_enviarTextoAFB_pulsado(View quien) {
        // cojo el mensaje que ha escrito el usuario
        String mensaje = "msg:" + this.txtInput.getText() + " :"
                + System.currentTimeMillis();
        // borro lo escrito
        this.txtInput.setText("");
        // cierro el soft-teclado
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(this.txtInput.getWindowToken(), 0);
        // llamo al método que publica
        enviarTextoAFacebook_async(mensaje);
    }

    private void publishtweet() {
        StatusesService statusesService = TwitterCore.getInstance().getApiClient(obtenerSesionDeTwitter()).getStatusesService();
        Call<Tweet> call = statusesService.update(txtInput.getText().toString(), null, null, null, null, null, null, null, null);
        call.enqueue(new Callback<Tweet>() {
            @Override
            public void success(Result<Tweet> result) {
                txtInput.setText("");
                Toast.makeText(THIS, "Tweet publicado: " + result.response.message(), Toast.LENGTH_LONG).show();
            }

            @Override
            public void failure(TwitterException e) {
                Toast.makeText(THIS, "No se pudo publicar el tweet: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void publishTweetWithImage() {
        File photo = null;
        try {
            // 1. Abrimos el fichero con la imagen
            // imagen que queremos enviar. Como necesitamos un path (para
            // TypedFile) debe estar fuera de /res o /assets porque estos
            // estan dentro del .apk y NO tiene path
            //create a file to write bitmap data
            String filename = "DSC_0001.png";
            photo = new File(this.getCacheDir(), filename);
            photo.createNewFile();

            //Convert bitmap to byte array
            Bitmap bmp = ((BitmapDrawable) imgImagen.getDrawable()).getBitmap();
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            bmp.compress(Bitmap.CompressFormat.PNG, 0 /*ignored for PNG*/, bos);
            byte[] bitmapdata = bos.toByteArray();

            //write the bytes in file
            FileOutputStream fos = new FileOutputStream(photo);
            fos.write(bitmapdata);
            fos.flush();
            fos.close();
        } catch (Exception e) {
            Log.d("miApp", "enviarImagen : excepcion: " + e.getMessage());
            return;
        }
        // 2. ponemos el fichero en un TypedFile
        TypedFile typedFile = new TypedFile("image/jpg", photo);
        // 3. obtenemos referencia al media service

        MediaService ms = TwitterCore.getInstance().getApiClient(obtenerSesionDeTwitter()).getMediaService();
        // 3.1 ponemos la foto en el request body de la petición
        okhttp3.RequestBody requestBody = okhttp3.RequestBody.create(MediaType.parse("image/png"), photo);
        // 4. con el media service: enviamos la foto a Twitter
        Call<Media> call1 = ms.upload(
                requestBody, // foto que enviamos
                null, null);
        call1.enqueue(new Callback<Media>() {
            @Override
            public void success(Result<Media> mediaResult) {
                // he tenido éxito:
                Toast.makeText(THIS, "imagen publicada: " + mediaResult.response.toString(), Toast.LENGTH_LONG);
                // 5. como he tenido éxito, la foto está en twitter, pero no en el
                // timeline (no se ve) he de escribir un tweet referenciando la foto
                // 6. obtengo referencia al status service
                StatusesService statusesService = TwitterCore.getInstance().getApiClient(obtenerSesionDeTwitter()).getStatusesService();
                // 7. publico un tweet
                Call<Tweet> call2 = statusesService.update("prueba de enviar imagen" + System.currentTimeMillis(), // mensaje del tweet
                        null, false, null, null, null, true, false,
                        "" + mediaResult.data.mediaId
                // string con los identicadores (hasta 4, separado por coma) de las imágenes
                // que quiero que aparezcan en este tweet. El mediaId referencia a la foto que acabo de subir previamente
                );
                call2.enqueue(new Callback<Tweet>() {
                    @Override
                    public void success(Result<Tweet> result) {
                        Toast.makeText(THIS, "Tweet publicado: " + result.response.message().toString(), Toast.LENGTH_LONG).show();
                    }

                    @Override
                    public void failure(TwitterException e) {
                        Toast.makeText(THIS, "No se pudo publicar el tweet:" + e.getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
            } // sucess ()

            @Override
            public void failure(TwitterException e) {
                // failure de call1
                Toast.makeText(THIS, "No se pudo publicar el tweet: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private TwitterSession obtenerSesionDeTwitter() {
        return TwitterCore.getInstance().getSessionManager().getActiveSession();
    }
}
