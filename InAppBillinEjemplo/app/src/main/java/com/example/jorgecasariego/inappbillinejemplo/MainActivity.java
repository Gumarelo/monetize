package com.example.jorgecasariego.inappbillinejemplo;

import android.content.Intent;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;

import com.example.jorgecasariego.inappbillinejemplo.util.IabHelper;
import com.example.jorgecasariego.inappbillinejemplo.util.IabResult;
import com.example.jorgecasariego.inappbillinejemplo.util.Inventory;
import com.example.jorgecasariego.inappbillinejemplo.util.Purchase;


public class MainActivity extends ActionBarActivity {

    private static final String TAG = "com.jorgecasariego.inappbillinejemplo";
    IabHelper mHelper;
    static final String ITEM_SKU = "android.test.purchased";

    private Button clickButton;
    private Button buyButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        buyButton = (Button)findViewById(R.id.buyButton);
        clickButton = (Button)findViewById(R.id.clickButton);
        clickButton.setEnabled(false);

        //String base64EncodedPublicKey = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAqwITXXp9Lp0xA890tPiDGL3ptNDcV/0EPVxuNzp0CjpZXac7b0hTkkllISrxiZZAYgVL3xnA/6j2S3JRwB660em3kOLFuJ549bFrOuQJmPdWOzIU8BVZ6XHtxzWjFbIUx/Gi+u209vx0LL8Dsmon8h//AFNhK/asXBChRngY0qvgdTSXoLun6BTrbWTlhXbzZKpSloTiGpPxKWFndo2Ddo0uGEziYwuj/XnYUv4yE7ElpS1oQdIir9GdgLo0+/gIfBWz2vR+6FcYoTyseB6I7bHdoMM+tpsra969xgrhQwSG13pSCM0IAJ4QCmAEQzE7BoQvp+KJSbZwm9YqP1qqdwIDAQAB";
        String base64EncodedPublicKey = "INSERT-YOUR-KEY-HERE"; //Ir a Google Play Developer console - Servicios y APIs y pega tu clave de licencia para esta aplicacion
        mHelper = new IabHelper(this, base64EncodedPublicKey);

        mHelper.startSetup(new IabHelper.OnIabSetupFinishedListener() {
            @Override
            public void onIabSetupFinished(IabResult result) {
                if(!result.isSuccess()){
                    Log.d(TAG, "In-app billing setup failed: " + result);
                } else {
                    Log.d(TAG, "In-app billing is set up OK");
                }
            }
        });

    }

    public void buttonClicked(View view){
        clickButton.setEnabled(false);
        buyButton.setEnabled(true);
    }

    public void buyClick(View view){
        /**
         * parametros:
         * 1. A reference to the enclosing Activity instance from which the method is being called
         * 2. The SKU that identifies the product that is being purchased.
         *    In this instance we are going to use a standard SKU provided by Google for testing purposes.
         *    This SKU, referred to as a static response SKU, will always result in a successful
         *    purchase. Other testing SKUs available for use when testing purchasing functionality
         *    without making real purchases are android.test.cancelled, android.test.refunded and
         *    android.test.item_unavailable.
         * 3. The request code which can be any positive integer value.
         *    When the purchase has completed, the onActivityResult method will be called and passed
         *    this integer along with the purchase response. This allows the method to identify which
         *    purchase process is returning and can be useful when the method needs to be able to
         *    handle purchasing for different items.
         * 4. The listener method to be called when the purchase is complete.
         * 5. The developer payload token string. This can be any string value and is used to identify
         *    the purchase. For the purposes of this example, this is set to “mypurchasetoken”.
         *
         */
        mHelper.launchPurchaseFlow(this, ITEM_SKU, 10001,
                mPurchaseFinishedListener, "mypurchasetoken");
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(!mHelper.handleActivityResult(requestCode, resultCode, data)){
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    /**
     * The “purchase finished” listener must perform a number of different tasks.
     * 1. In the first instance, it must check to ensure that the purchase was successful.
     * 2. t then needs to check the SKU of the purchased item to make sure it matches the one specified in the purchase request.
     * 3. In the event of a successful purchase, the method will need to consume the purchase so that
     * the user can purchase it again when another one is needed. If the purchase is not consumed,
     * future attempts to purchase the item will fail stating that the item has already been purchased.
     * Whilst this would be desired behavior if the user only needed to purchase the item once, clearly
     * this is not the behavior required for consumable purchases. Finally, the method needs to enable
     * the “Click Me!” button so that the user can perform the button click that was purchased.
     */
    IabHelper.OnIabPurchaseFinishedListener mPurchaseFinishedListener =
            new IabHelper.OnIabPurchaseFinishedListener(){
                public void onIabPurchaseFinished(IabResult result, Purchase purchase){
                    if(result.isFailure()){
                        //Handle error
                        return;
                    } else if(purchase.getSku().equals(ITEM_SKU)){
                        consumeItem();
                        buyButton.setEnabled(true);
                    }
                }
            };

    /**
     * ConsumeItem
     * In the documentation for Google Play In-app Billing, Google recommends that consumable items
     * be consumed before providing the user with access to the purchased item.
     *
     *  In the event of a successful purchase, the mPurchaseFinishedListener implementation has been
     *  configured to call a method named consumeItem(). It will be the responsibility of this method
     *  to query the billing system to make sure that the purchase has been made.
     *
     *  This involves making a call to the queryInventoryAsync() method of the mHelper object.
     *  This task is performed asynchronously from the application’s main thread and a listener method
     *  called when the task is complete. If the item has been purchased, the listener will consume
     *  the item via a call to the consumeAsync() method of the mHelper object.
     */
    public void consumeItem(){
        mHelper.queryInventoryAsync(mReceivedInventoryListener);
    }

    IabHelper.QueryInventoryFinishedListener mReceivedInventoryListener =
            new IabHelper.QueryInventoryFinishedListener() {
                @Override
                public void onQueryInventoryFinished(IabResult result, Inventory inventory) {
                    if(result.isFailure()) {
                        //Handle failure
                    } else {
                        mHelper.consumeAsync(inventory.getPurchase(ITEM_SKU), mConsumeFinishedListener);
                    }
                }
            };

    IabHelper.OnConsumeFinishedListener mConsumeFinishedListener =
            new IabHelper.OnConsumeFinishedListener() {
                public void onConsumeFinished(Purchase purchase,
                                              IabResult result) {

                    if (result.isSuccess()) {
                        clickButton.setEnabled(true);
                    } else {
                        // handle error
                    }
                }
            };

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if(mHelper != null){
            mHelper.dispose();
        }
        mHelper = null;
    }
}
