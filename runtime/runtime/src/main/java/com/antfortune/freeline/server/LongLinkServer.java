package com.antfortune.freeline.server;


import android.content.Context;
import android.util.Log;

import com.antfortune.freeline.router.Router;
import com.antfortune.freeline.router.schema.CheckResourceSchema;
import com.antfortune.freeline.router.schema.CheckSyncSchema;
import com.antfortune.freeline.router.schema.CloseLonglinkSchema;
import com.antfortune.freeline.router.schema.PushDexSchema;
import com.antfortune.freeline.router.schema.PushFullResourcePackSchema;
import com.antfortune.freeline.router.schema.PushResourceSchema;
import com.antfortune.freeline.router.schema.RestartSchema;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

public class LongLinkServer extends EmbedHttpServer {

    private static final String TAG = "Freeline.LongLinkServer";

    public static final int PORT_FROM = 41128;

    public final Context context;

    private static LongLinkServer sServer;

    private static Router sRouter;

    private static boolean hasResChange;

    private static boolean hasDexChange;

    private LongLinkServer(Context ctx, int port) {
        super(port);
        this.context = ctx;
    }

    protected void handle(String method, String path, HashMap<String, String> headers, Map<String, String> queries, InputStream input, ResponseOutputStream response) throws Exception {
        if (sRouter != null) {
            boolean result = sRouter.dispatch(method, path, headers, queries, input, response);
            if (result) {
                return;
            }

            response.setContentTypeText();
            response.write(("miss schema: " + path).getBytes("utf-8"));
            response.setStatusCode(500);
            return;
        }

        super.handle(method, path, headers, queries, input, response);
    }

    public static void markResourcesChanged() {
        hasResChange = true;
    }

    public static void resetResourcesChangedFlag() {
        hasResChange = false;
    }

    public static boolean isResourcesChanged() {
        return hasResChange;
    }

    public static void markDexChanged() {
        hasDexChange = true;
    }

    public static boolean isDexChanged() {
        return hasDexChange;
    }

    public static void resetDexChangedFlag() {
        hasDexChange = false;
    }

    public static void start(Context ctx, Router router) {
        if (sServer != null) {
            Log.d(TAG, "Freeline.increment server is already running");
            return;
        }

        for (int i = 0; i < 100; i++) {
            LongLinkServer s = new LongLinkServer(ctx, PORT_FROM + i);
            try {
                s.start();
                sServer = s;
                sRouter = router;
                initRouter();
                Log.d(TAG, "Freeline.increment server running on port " + (PORT_FROM + i));
                break;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private static void initRouter() {
        sRouter.registerSchema(new CheckSyncSchema());
        sRouter.registerSchema(new CheckResourceSchema());
        sRouter.registerSchema(new CloseLonglinkSchema());
        sRouter.registerSchema(new PushDexSchema());
        sRouter.registerSchema(new PushFullResourcePackSchema());
        sRouter.registerSchema(new PushResourceSchema());
        sRouter.registerSchema(new RestartSchema());
    }


    private static String byteArrayToHex(byte[] a) {
        StringBuilder sb = new StringBuilder(a.length * 2);
        for (byte b : a)
            sb.append(String.format("%02x", new Object[]{Integer.valueOf(b & 0xFF)}));
        return sb.toString();
    }
}