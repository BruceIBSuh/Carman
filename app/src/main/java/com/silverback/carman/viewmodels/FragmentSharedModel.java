package com.silverback.carman.viewmodels;

import android.util.SparseArray;
import android.util.SparseIntArray;
import android.util.SparseLongArray;

import androidx.fragment.app.Fragment;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.silverback.carman.database.FavoriteProviderEntity;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Calendar;
import java.util.List;

public class FragmentSharedModel extends ViewModel {

    private final MutableLiveData<Fragment> fragment = new MutableLiveData<>();
    private final MutableLiveData<SparseIntArray> numpadValue = new MutableLiveData<>();
    private final MutableLiveData<SparseArray<String>> memopadValue = new MutableLiveData<>();
    private final MutableLiveData<SparseArray<Object>> svcLocation = new MutableLiveData<>();

    private final MutableLiveData<Boolean> alertGasResult = new MutableLiveData<>();
    private final MutableLiveData<Boolean> alertSvcResult = new MutableLiveData<>();

    // ExpenseServiceFragment and RegisterDialogFragment
    private MutableLiveData<SparseArray<Object>> serviceLocation;
    private MutableLiveData<String> registeredServiceId;

    // SettingSvcItemDlgFragment and SettingServiceItemFragment
    private MutableLiveData<JSONObject> jsonServiceItemObj;

    private MutableLiveData<Boolean> alertPostResult;
    private MutableLiveData<String> newPosting;
    private MutableLiveData<Integer> removedPosting;
    private MutableLiveData<Integer> editedPosting;
    private MutableLiveData<SparseLongArray> newComment;

    private MutableLiveData<FavoriteProviderEntity> favoriteGasEntity;
    private MutableLiveData<FavoriteProviderEntity> favoriteSvcEntity;
    private MutableLiveData<Integer> fragmentIndex;
    private MutableLiveData<String> favoriteStnId;
    private MutableLiveData<Integer> imageItemSelected;
    private MutableLiveData<Integer> imageChooser;
    private MutableLiveData<String> strData;
    private MutableLiveData<String> firstPlaceholderId;

    //private MutableLiveData<Integer> totalGasExpense, totalSvcExpense;
    private MutableLiveData<Integer> totalExpense;
    private MutableLiveData<Integer> expenseCategory;


    // DatePicker, TimePickerFragment
    private MutableLiveData<Calendar> customDateAndTime;

    // AutoData used in SettingPreferenceActivity which is shared b/w SettingPrefereneFragment and
    // SettingAutoFragment
    private MutableLiveData<JSONArray> jsonAutoDataArray;
    private MutableLiveData<Boolean> jsonAutoData;

    // Pass the Sido and Sigun name fetched in SettingSpinnerDlgFragment to SettingPrefernceFragment
    // to show the names in the summary of the preference.
    private MutableLiveData<List<String>> defaultDistCode;
    // PermissionDialogFragment and GeneralFragment to pass the dialog click event.
    private MutableLiveData<Boolean> permission;

    private MutableLiveData<String> engineType;

    // Communicate b/w ExpensePagerFragment and a fragment contained in the tab viewpager
    public MutableLiveData<Integer> getCurrentFragment() {
        if(fragmentIndex == null) fragmentIndex = new MutableLiveData<>();
        return fragmentIndex;
    }
    // Share ExpenseGasFragment and ExpenseServiceFragment with NumPadFragment
    public void setNumPadValue(int key, int value) {
        SparseIntArray sparsesArray = new SparseIntArray(1); //param: initial capacity.
        sparsesArray.put(key, value);
        numpadValue.setValue(sparsesArray);
    }

    public LiveData<SparseIntArray> getNumpadValue() {
        return numpadValue;
    }

    public MutableLiveData<String> getRegisteredServiceId() {
        if(registeredServiceId == null) registeredServiceId = new MutableLiveData<>();
        return registeredServiceId;
    }
    


    // Pass a String value in MemoPadFragment to ExpenseServiceFragment
    public void setMemoPadValue(int key, String value) {
        SparseArray<String> sparseArray = new SparseArray<>(1);
        sparseArray.put(key, value);
        memopadValue.setValue(sparseArray);
    }
    public LiveData<SparseArray<String>> getMemoPadValue() {
        return memopadValue;
    }


    // Communicate b/w SettingServiceItemFragment and SettingSvcItemDlgFragment to modify the
    // service item list.
    public MutableLiveData<JSONObject> getJsonServiceItemObj() {
        if(jsonServiceItemObj == null) jsonServiceItemObj = new MutableLiveData<>();
        return jsonServiceItemObj;
    }

    public MutableLiveData<Boolean> getAlertPostResult() {
        if(alertPostResult == null) alertPostResult = new MutableLiveData<>();
        return alertPostResult;
    }



    // Commmunicate b/w RegisterDialogFragment and ExpenseServiceFragment
    public void setServiceLocation(SparseArray<Object> sparseArray) {
        svcLocation.setValue(sparseArray);
    }
//    public LiveData<SparseArray<Object>> getServiceLocation() {
//        return svcLocation;
//    }

    public MutableLiveData<SparseArray<Object>> getServiceLocation(){
        if(serviceLocation == null) serviceLocation = new MutableLiveData<>();
        return serviceLocation;
    }


    public MutableLiveData<FavoriteProviderEntity> getFavoriteGasEntity() {
        if(favoriteGasEntity == null) favoriteGasEntity = new MutableLiveData<>();
        return favoriteGasEntity;
    }

    public MutableLiveData<FavoriteProviderEntity> getFavoriteSvcEntity() {
        if(favoriteSvcEntity == null) favoriteSvcEntity = new MutableLiveData<>();
        return favoriteSvcEntity;
    }

    public MutableLiveData<String> getFavoriteStnId() {
        if(favoriteStnId == null) favoriteStnId = new MutableLiveData<>();
        return favoriteStnId;
    }

    // Communicate ExpenseGasFragment or ExpenseServiceFragment w/ AlertDidalogFragment when
    // the favorite button clicks to remove a provider out of the favorite list.
    public void setAlertGasResult(boolean b) {
        alertGasResult.setValue(b);
    }
    public MutableLiveData<Boolean> getAlertGasResult() {
        return alertGasResult;
    }
    public void setAlertSvcResult(boolean b) {
        alertSvcResult.setValue(b);
    }

    // Communicate b/w BoardWriteFragment and BoardChooserDlgFragme to notify which image chooser
    // out of camera or gallery is selected.
    public MutableLiveData<Integer> getImageChooser() {
        if(imageChooser == null) imageChooser = new MutableLiveData<>();
        return imageChooser;
    }

    // Communicate b/w BoardWriteFragment and BoardPagerFragment both of which BoardActivity
    // cocntains.

    public MutableLiveData<String> getNewPosting() {
        if(newPosting == null) newPosting = new MutableLiveData<>();
        return newPosting;
    }

    // Communicate b/w AlertDialogFragment and fragment calling the dialog fragment
    public MutableLiveData<Integer> getRemovedPosting() {
        if(removedPosting == null) removedPosting = new MutableLiveData<>();
        return removedPosting;
    }

    // Communicate b/w BoardEditFragment and BoardPagerFragment
    public MutableLiveData<Integer> getEditedPosting() {
        if(editedPosting == null) editedPosting = new MutableLiveData<>();
        return editedPosting;
    }

    // Communicate b/w BoardPagerFragment and BoardReadFragment to pass a new comment in a post.
    public MutableLiveData<SparseLongArray> getNewComment() {
        if(newComment == null) newComment = new MutableLiveData<>();
        return newComment;
    }



    // Commnumcate b/w GeneralFragment and MainPricePagerFragment in MainActivity to pass the station id
    // of the first placeholder.
    public MutableLiveData<String> getFirstPlaceholderId() {
        if(firstPlaceholderId == null) firstPlaceholderId = new MutableLiveData<>();
        return firstPlaceholderId;
    }


    public MutableLiveData<List<String>> getDefaultDistrict() {
        if(defaultDistCode == null) defaultDistCode = new MutableLiveData<>();
        return defaultDistCode;
    }


    // Shared the selected spinner item position in ExpenseStmtsFragment with ExpenseGraphFragment to set
    // data queried by category.
    public MutableLiveData<Integer> getTotalExpenseByCategory() {
        if(totalExpense == null)  totalExpense = new MutableLiveData<>();
        return totalExpense;
    }

    public MutableLiveData<Integer> getExpenseCategory() {
        if(expenseCategory == null) expenseCategory = new MutableLiveData<>();
        return expenseCategory;
    }


    public MutableLiveData<Boolean> getJsonAutoData() {
        if(jsonAutoData == null) jsonAutoData = new MutableLiveData<>();
        return jsonAutoData;
    }

    public MutableLiveData<JSONArray> getAutoData() {
        if(jsonAutoDataArray == null) jsonAutoDataArray = new MutableLiveData<>();
        return jsonAutoDataArray;
    }

    public MutableLiveData<Boolean> getPermission() {
        if(permission == null) permission = new MutableLiveData<>();
        return permission;
    }

    // Communicate b/w ExpenseGasFragment/ExpenseServiceFragment and
    // DatePickerFragment/TimePickerFragment
    public MutableLiveData<Calendar> getCustomDateAndTime() {
        if(customDateAndTime == null) customDateAndTime = new MutableLiveData<>();
        return customDateAndTime;
    }

    // SetttingAutoFragment and EngineDialogFragment defined as static inner class
    public MutableLiveData<String> getEngineSelected() {
        if(engineType == null) engineType = new MutableLiveData<>();
        return engineType;
    }
}
