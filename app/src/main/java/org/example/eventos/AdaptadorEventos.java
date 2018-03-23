package org.example.eventos;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.firebase.ui.firestore.FirestoreRecyclerAdapter;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;

import java.io.InputStream;

/**
 * Created by Miguel Á. Núñez on 20/03/2018.
 */

public class AdaptadorEventos extends FirestoreRecyclerAdapter<Evento, AdaptadorEventos.ViewHolder> {
    protected View.OnClickListener onClickListener;

    public AdaptadorEventos(@NonNull FirestoreRecyclerOptions<Evento> options) {
        super(options);
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.evento, parent, false);
        view.setOnClickListener(onClickListener);
        return new ViewHolder(view);
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        public TextView txtEvento, txtCiudad, txtFecha;
        public ImageView imagen;

        public ViewHolder(View itemView) {
            super(itemView);
            txtEvento = (TextView) itemView.findViewById(R.id.txtEvento);
            txtCiudad = (TextView) itemView.findViewById(R.id.txtCiudad);
            txtFecha = (TextView) itemView.findViewById(R.id.txtFecha);
            imagen = (ImageView) itemView.findViewById(R.id.imgImagen);
        }
    }

    @Override
    protected void onBindViewHolder(@NonNull ViewHolder holder, int position, @NonNull Evento item) {
        holder.txtEvento.setText(item.getEvento());
        holder.txtCiudad.setText(item.getCiudad());
        holder.txtFecha.setText(item.getFecha());
        new DownloadImageTask((ImageView) holder.imagen).execute(item.getImagen());
        holder.itemView.setOnClickListener(onClickListener);
    }

    public void setOnItemClickListener(View.OnClickListener onClick) {
        onClickListener = onClick;
    }

    private class DownloadImageTask extends AsyncTask<String, Void, Bitmap> {
        ImageView imageView;

        public DownloadImageTask(ImageView bmImage) {
            this.imageView = bmImage;
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
            imageView.setImageBitmap(result);
        }
    }
}
