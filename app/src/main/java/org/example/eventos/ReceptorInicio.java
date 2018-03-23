package org.example.eventos;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * Created by Miguel Á. Núñez on 20/03/2018.
 */

public class ReceptorInicio extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        context.startService(new Intent(context, EventosFCMService.class));
    }
}