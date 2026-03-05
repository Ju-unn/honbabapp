package com.example.honbabapp.activity.meeting;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.widget.RadioButton;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.honbabapp.activity.MainActivity;
import com.example.honbabapp.api.ApiService;
import com.example.honbabapp.api.RetrofitClient;
import com.example.honbabapp.api.data.request.MeetingCreateRequest;
import com.example.honbabapp.api.data.response.ApiResponse;
import com.example.honbabapp.api.data.response.meeting.MeetingCreateResponse;
import com.example.honbabapp.databinding.PageMeetingCardWritingBinding;
import com.example.honbabapp.ui.fragment.HomeFragment;
import com.google.android.material.slider.RangeSlider;

import java.text.NumberFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * 모임 생성 Activity
 *
 * 기능:
 * 1. 모임 정보 입력
 * 2. create.php API 호출
 * 3. 모임 생성 완료
 */

public class MeetingCardWritingActivity extends AppCompatActivity{

    private String TAG = "MeetingCardWriting";
    private PageMeetingCardWritingBinding binding;
    private ApiService apiService;

    private int userId;
    private String selectedCategory = "한식";
    private String selectedDate = "";
    private String selectedTime = "";
    private int participantCount = 2;

    private int minPrice = 0;  // 기본 최소: 10,000원
    private int maxPrice = 100000;  // 기본 최대: 30,000원

    // 선택한 장소 정보
    private String selectedPlaceName = "";
    private String selectedAddress = "";
    private String selectedRoadAddress = "";
    private String selectedSigungu = "";
    private String selectedDong = "";
    private String selectedMapX = "";
    private String selectedMapY = "";
    private String selectedPlaceCategory = "";
    private String selectedPhone = "";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = PageMeetingCardWritingBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Api 서비스 초기화
        apiService = RetrofitClient.getClient().create(ApiService.class);

        // 현재 사용자 ID
        SharedPreferences prefs = getSharedPreferences("honbabapp_prefs" , MODE_PRIVATE);
        userId = prefs.getInt("user_id" , -1);

        // UI 설정
        setupUI();
    }

    /**
     * UI 초기 설정
     */
    private void setupUI() {
        // 뒤로가기 버튼
        binding.btnBack.setOnClickListener(v -> finish());

        // 카테고리 선택
        setupCategorySelection();

        // 날짜 선택
        setupDatePicker();

        // 시간 선택
        setupTimePicker();

        // 인원 증감 버튼
        setupParticipantCounter();

        // RangeSlider 설정
        setupPriceRangeSlider();

        // 장소 선택 버튼
        setupPlaceSelection();

        // 모임 생성 버튼
        binding.btnCreateMeeting.setOnClickListener(v -> validateAndCreateMeeting());
    }

    /**
     * ActivityResultLauncher
     */
    private final ActivityResultLauncher<Intent> placeSearchLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.StartActivityForResult() ,
                    result -> {
                        if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                            Intent data = result.getData();
                            // 선택한 장소 정보 받기
                            selectedPlaceName = data.getStringExtra("place_name");
                            selectedAddress = data.getStringExtra("address");
                            selectedRoadAddress = data.getStringExtra("road_address");
                            selectedSigungu = data.getStringExtra("sigungu");
                            selectedDong = data.getStringExtra("dong");
                            selectedMapX = data.getStringExtra("map_x");
                            selectedMapY = data.getStringExtra("map_y");
                            selectedPlaceCategory = data.getStringExtra("category");
                            selectedPhone = data.getStringExtra("phone");

                            // UI 업데이트
                            binding.tvSelectedStore.setText(selectedPlaceName);
                            binding.tvSelectedStoreAddress.setText(selectedRoadAddress);

                            Log.i(TAG, "선택한 장소: " + selectedPlaceName);
                            Log.i(TAG, "주소: " + selectedRoadAddress);
                            Log.i(TAG, "좌표: (" + selectedMapX + ", " + selectedMapY + ")");
                        }
                    }
            );

    /**
     * 장소 선택 버튼 설정
     */
    private void setupPlaceSelection() {
        // 장소 선택하기 버튼
        binding.btnSearchStore.setOnClickListener(v -> {
            Intent intent = new Intent(this , PlaceSearchActivity.class);
            placeSearchLauncher.launch(intent);
        });
    }

    /**
     * 카테고리 설정
     */
    private void setupCategorySelection() {
        RadioButton[] buttons = {
                binding.radioCategoryKorean,
                binding.radioCategoryChinese,
                binding.radioCategoryJapanese ,
                binding.radioCategoryWestern ,
                binding.radioCategoryOmakase ,
                binding.radioCategoryCafe ,
                binding.radioCategoryPub
        };

        String[] categoryTexts = {
                "한식", "중식", "일식", "양식", "오마카세", "카페", "술집"
        };


        for (int i = 0; i < buttons.length; i++) {
            final int index = i;
            buttons[i].setOnClickListener(v -> {
                // 모든 버튼 선택 해제
                for (RadioButton button : buttons) {
                    button.setChecked(false);
                }

                // 클릭한 버튼만 선택
                buttons[index].setChecked(true);
                selectedCategory = categoryTexts[index];

                Log.i(TAG, "선택된 카테고리: " + selectedCategory);
            });
        }

        // 기본 선택 (한식)
        binding.radioCategoryKorean.setChecked(true);
        selectedCategory = "한식";
    }

    /**
     * 날짜 선택 DatePicker
     */
    private void setupDatePicker() {
        binding.etMeetingDate.setOnClickListener(v -> {
            Calendar calendar = Calendar.getInstance();
            int year = calendar.get(Calendar.YEAR);
            int month = calendar.get(Calendar.MONTH);
            int day = calendar.get(Calendar.DAY_OF_MONTH);

            DatePickerDialog datePickerDialog = new DatePickerDialog(this , (view , selectedYear , selectedMonth , selectedDay) -> {
                // 날짜 저장
                selectedDate = String.format("%04d-%02d-%02d" , selectedYear , selectedMonth + 1 , selectedDay);

                //화면 표시
                String displayDate = String.format("%d월  %d일" , selectedMonth + 1 ,selectedDay);
                binding.etMeetingDate.setText(displayDate);

                Log.i(TAG, "선택된 날짜: " + selectedDate);
            } ,
                    year , month , day);

            // 오늘 이전 날짜 선택 불가
            datePickerDialog.getDatePicker().setMinDate(System.currentTimeMillis());
            datePickerDialog.show();
        });
    }

    /**
     * 시간 선택 TimePicker
     */
    private void setupTimePicker() {
        binding.etMeetingTime.setOnClickListener(v -> {
            Calendar calendar = Calendar.getInstance();
            int hour = calendar.get(Calendar.HOUR_OF_DAY);
            int minute = calendar.get(Calendar.MINUTE);

            TimePickerDialog timePickerDialog = new TimePickerDialog(this , (view , selectedHour , selectedMinute) -> {
                // 시간 저장
                selectedTime = String.format("%02d:%02d:00" , selectedHour , selectedMinute);

                // 화면 표시 (오전/오후 형식)
                String ampm = selectedHour >= 12 ? "오후" : "오전";
                int displayHour = selectedHour > 12 ? selectedHour - 12 :
                        (selectedHour == 0 ? 12 : selectedHour);
                String displayTime = String.format("%s %d:%02d", ampm, displayHour, selectedMinute);
                binding.etMeetingTime.setText(displayTime);

                Log.i(TAG, "선택된 시간: " + selectedTime);
            },
                    hour , minute , false);
            timePickerDialog.show();
        });
    }

    /**
     * 참여 인원 증감 버튼
     */
    private void setupParticipantCounter() {
        // 현재 인원 표시
        updateParticipantCount();

        // 감소 버튼
        binding.btnDecreaseCount.setOnClickListener(v -> {
            if (participantCount > 2) {
                participantCount --;
                updateParticipantCount();
            }
        });

        //증가 버튼
        binding.btnIncreaseCount.setOnClickListener(v -> {
            if (participantCount < 10) {
                participantCount ++;
                updateParticipantCount();
            }
        });
    }


    /**
     * 참여 인원 텍스트 업데이트
     */
    private void updateParticipantCount() {
        binding.tvParticipantCount.setText(participantCount + "명");
    }

    /**
     * RangeSlider 설정
     */
    private void setupPriceRangeSlider() {
        // 초기 가격 표시
        updatePriceRangeDisplay(minPrice , maxPrice);

        // RangeSlider 값 변경 리스너
        binding.rangeSliderPrice.addOnChangeListener(new RangeSlider.OnChangeListener() {
            @Override
            public void onValueChange(@NonNull RangeSlider slider, float value, boolean fromUser) {
                // Slider 값 가져오기
                List<Float> values = slider.getValues();
                if (values.size() == 2) {
                    minPrice = values.get(0).intValue();
                    maxPrice = values.get(1).intValue();

                    // 화면 업데이트
                    updatePriceRangeDisplay(minPrice , maxPrice);
                    Log.i(TAG, "가격대 변경: " + minPrice + "원 ~ " + maxPrice + "원");
                }
            }
        });
    }

    /**
     * 가격대 표시 업데이트
     */
    private void updatePriceRangeDisplay(int min , int max) {
        NumberFormat formatter = NumberFormat.getInstance(Locale.KOREA);
        String displayText = formatter.format(min) + "원 ~ " + formatter.format(max) + "원";
        binding.tvPriceRangeDisplay.setText(displayText);
    }

    /**
     * 입력 검증 및 모임 생성
     */
    private void validateAndCreateMeeting() {
        // 모임명 검증
        String title = binding.etMeetingTitle.getText().toString().trim();
        if (title.isEmpty()) {
            Toast.makeText(this, "모임명을 입력해주세요", Toast.LENGTH_SHORT).show();
            return;
        }
        // 모임 설명 검증
        String description = binding.etMeetingDescription.getText().toString().trim();
        if (description.isEmpty()) {
            Toast.makeText(this, "모임 설명을 입력해주세요", Toast.LENGTH_SHORT).show();
            return;
        }

        // 날짜 검증
        if (selectedDate.isEmpty()) {
            Toast.makeText(this, "날짜를 선택해주세요", Toast.LENGTH_SHORT).show();
            return;
        }

        // 시간 검증
        if (selectedTime.isEmpty()) {
            Toast.makeText(this, "시간을 선택해주세요", Toast.LENGTH_SHORT).show();
            return;
        }

        // 가격대 검증
        NumberFormat formatter = NumberFormat.getInstance(Locale.KOREA);
        String priceRange = formatter.format(minPrice) + "원 ~ " + formatter.format(maxPrice) + "원";


        // 장소 검증 (선택 안했으면 경고)
        if (selectedPlaceName.isEmpty()) {
            Toast.makeText(this, "장소를 선택해주세요", Toast.LENGTH_SHORT).show();
            return;
        }

        // TODO: 미팅 사진 저장 해야함

        // 모임 생성 API 호출
        createMeeting(title, description, selectedDate, selectedTime, priceRange);

    }

    /**
     * 모임 생성 API 호출
     */
    private void createMeeting(String title, String description, String date, String time,
                               String priceRange) {

        // 버튼 비활성화
        binding.btnCreateMeeting.setEnabled(false);
        binding.btnCreateMeeting.setText("생성 중...");

        // datetime 조합
        String datetime = date + " " + time;

        // LocationRequest 생성
        MeetingCreateRequest.LocationRequest locationRequest =
                new MeetingCreateRequest.LocationRequest(
                        selectedPlaceName,      // place_name
                        selectedAddress,        // address
                        selectedRoadAddress,    // road_address
                        selectedSigungu,        // sigungu
                        selectedDong,           // dong
                        selectedMapX,           // map_x
                        selectedMapY,           // map_y
                        selectedPlaceCategory,  // category
                        selectedPhone           // phone_number
                );

        // MeetingCreateRequest 생성
        MeetingCreateRequest request = new MeetingCreateRequest(
                userId ,
                title ,
                description ,
                selectedCategory ,
                datetime ,
                participantCount ,
                priceRange ,
                locationRequest
        );

        //API 호출
        Call<ApiResponse<MeetingCreateResponse>> call = apiService.createMeeting(request);

        call.enqueue(new Callback<ApiResponse<MeetingCreateResponse>>() {
            @Override
            public void onResponse(Call<ApiResponse<MeetingCreateResponse>> call,
                                   Response<ApiResponse<MeetingCreateResponse>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    ApiResponse<MeetingCreateResponse> apiResponse = response.body();

                    if (apiResponse.isSuccess()) {
                        MeetingCreateResponse data = apiResponse.getData();
                        int meetingId = data.getMeeting_id();

                        Log.i(TAG, "모임 생성 성공: meeting_id=" + meetingId);

                        // 성공 메시지
                        Toast.makeText(MeetingCardWritingActivity.this,
                                "모임이 생성되었습니다!",
                                Toast.LENGTH_SHORT).show();

                        // 생성된 모임 상세 화면으로 이동
                        Intent intent = new Intent(MeetingCardWritingActivity.this, MainActivity.class);
                        intent.putExtra("refresh_home", true);
                        intent.putExtra("new_meeting_id", meetingId);
                        startActivity(intent);
                        finish();

                    } else {
                        Log.e(TAG, "API 실패: " + apiResponse.getMessage());
                        Toast.makeText(MeetingCardWritingActivity.this,
                                apiResponse.getMessage(),
                                Toast.LENGTH_SHORT).show();

                        // 버튼 다시 활성화
                        binding.btnCreateMeeting.setEnabled(true);
                        binding.btnCreateMeeting.setText("모임 생성");
                    }
                } else {
                    Log.e(TAG, "HTTP 오류: " + response.code());
                    Toast.makeText(MeetingCardWritingActivity.this,
                            "모임 생성에 실패했습니다.",
                            Toast.LENGTH_SHORT).show();

                    // 버튼 다시 활성화
                    binding.btnCreateMeeting.setEnabled(true);
                    binding.btnCreateMeeting.setText("모임 생성");
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<MeetingCreateResponse>> call, Throwable t) {
                Log.e(TAG, "네트워크 오류: " + t.getMessage());
                Toast.makeText(MeetingCardWritingActivity.this,
                        "네트워크 오류가 발생했습니다.",
                        Toast.LENGTH_SHORT).show();

                // 버튼 다시 활성화
                binding.btnCreateMeeting.setEnabled(true);
                binding.btnCreateMeeting.setText("모임 생성");
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }

}





































