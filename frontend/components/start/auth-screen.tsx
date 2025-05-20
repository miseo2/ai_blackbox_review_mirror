"use client"

import type React from "react"

import { useState, useRef, useEffect } from "react"
import { motion } from "framer-motion"
import Image from "next/image"
import { useRouter } from "next/navigation"
import { ChevronLeft, ChevronRight } from "lucide-react"
import { CapacitorKakaoLogin } from '@team-lepisode/capacitor-kakao-login'
import { Preferences } from '@capacitor/preferences'
import { App } from '@capacitor/app'
import { registerFcmToken } from '@/lib/api/Fcm' // FCM 토큰 등록 유틸리티 import
import { Capacitor } from '@capacitor/core'

export default function AuthScreen() {
  const [currentSlide, setCurrentSlide] = useState(0)
  const [isLoading, setIsLoading] = useState(false)
  const [touchStart, setTouchStart] = useState(0)
  const [touchEnd, setTouchEnd] = useState(0)
  const sliderRef = useRef<HTMLDivElement>(null)
  const router = useRouter()

  // 1) SDK 초기화
  useEffect(() => {
    CapacitorKakaoLogin.initialize({
      appKey: process.env.NEXT_PUBLIC_KAKAO_NATIVE_APP_KEY!,  // "네이티브 앱 키"
    }).catch(e => console.error('SDK init 에러', e))
  }, []);

  // 앱 상태 변경 리스너 추가
  useEffect(() => {
    let cleanup: (() => void) | undefined;
    
    const setupListener = async () => {
      try {
        const listener = await App.addListener('appStateChange', ({ isActive }) => {
          if (isActive) {
            // 앱이 다시 활성화될 때 필요한 로직 추가
            Preferences.get({ key: 'kakao_access_token' }).then(({ value }) => {
              if (value) {
                console.log('저장된 카카오 토큰 발견');
              }
            });
          }
        });
        
        cleanup = () => {
          listener.remove();
        };
      } catch (error) {
        console.error('앱 상태 리스너 등록 실패:', error);
      }
    };
    
    setupListener();
    
    return () => {
      if (cleanup) {
        cleanup();
      }
    };
  }, []);

  const slides = [
    {
      title: "설치만 하세요, 나머지는 자동으로",
      description: "앱을 설치해 두면 사고 발생 시 블랙박스 영상을 자동으로 감지하고 분석 보고서를 생성합니다.",
      images: [
        "/image/login.jpg",
      ],
    },
    {
      title: "실시간 분석, 즉각적인 결과",
      description: "블랙박스 영상을 업로드하면 AI가 즉시 분석하여 결과를 제공합니다.",
      images: [
        "image/robot.png"
      ],
    },
    {
      title: "법적 근거와 함께 제공되는 결과",
      description: "모든 분석 결과는 대한민국 교통법규에 기반한 법적 근거와 함께 제공됩니다.",
      images: [
        "/image/law.jpg"
      ],
    },
  ]

  // 터치 이벤트 핸들러
  const handleTouchStart = (e: React.TouchEvent) => {
    setTouchStart(e.targetTouches[0].clientX)
  }

  const handleTouchMove = (e: React.TouchEvent) => {
    setTouchEnd(e.targetTouches[0].clientX)
  }

  const handleTouchEnd = () => {
    if (touchStart - touchEnd > 75) {
      // 왼쪽으로 스와이프
      handleNextSlide()
    }

    if (touchStart - touchEnd < -75) {
      // 오른쪽으로 스와이프
      handlePrevSlide()
    }
  }

  const handleNextSlide = () => {
    setCurrentSlide((prev) => (prev + 1) % slides.length)
  }

  const handlePrevSlide = () => {
    setCurrentSlide((prev) => (prev === 0 ? slides.length - 1 : prev - 1))
  }

  // 다음에 하기 버튼 - 대시보드로 이동
  const handleSkip = () => {
    // 비로그인 상태로 대시보드 접근을 위해 임시 토큰 저장
    localStorage.setItem("guest_token", "temporary_access")
    router.push("/dashboard")
  }

  const handleKakaoLogin = async () => {
    console.log('📱📱📱 AuthScreen에서 앱 직접 로그인 방식 실행됨');
    console.log('[AuthScreen] 🔥 handleKakaoLogin 호출됨');
    setIsLoading(true);

    try {
      // 1️⃣ 플러그인으로 카카오 로그인 → accessToken, refreshToken 획득
      const result = await CapacitorKakaoLogin.login();
      console.log('[AuthScreen] 카카오 로그인 결과:', result);
      
      if (!result || !result.accessToken) {
        throw new Error('카카오 로그인 결과에 accessToken이 없습니다.');
      }
      
      const { accessToken, refreshToken } = result;
      console.log('[AuthScreen] 🎉 Kakao accessToken:', accessToken.substring(0, 10) + '...');

      await Preferences.set({
        key: "kakao_access_token",
        value: accessToken,
      });
      console.log('[AuthScreen] 🎉 카카오 토큰 저장 성공');

      // 2️⃣ 우리 서비스 백엔드에 POST 요청 (authToken 발급)
      const backendUrl = process.env.NEXT_PUBLIC_BACKEND_URL || 'https://k12e203.p.ssafy.io/api';
      console.log('[AuthScreen] 백엔드 URL:', backendUrl);
      
      console.log('[AuthScreen] 백엔드 요청 시작');
      const res = await fetch(
        `${backendUrl}/oauth/kakao/callback`,
        {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json',
          },
          body: JSON.stringify({
            accessToken,
            // refreshToken: refreshToken  // 필요한 경우
          }),
        }
      );
      
      console.log('[AuthScreen] 백엔드 응답 상태:', res.status, res.statusText);
      
      if (!res.ok) {
        const errorText = await res.text();
        console.error('[AuthScreen] 백엔드 오류 응답:', errorText);
        throw new Error(`백엔드 에러 ${res.status}: ${errorText}`);
      }
      
      const data = await res.json();
      console.log('[AuthScreen] 백엔드 응답 데이터:', JSON.stringify(data));
      
      if (!data || !data.authToken) {
        console.error('[AuthScreen] 백엔드 응답에 authToken이 없습니다:', data);
        throw new Error('백엔드 응답에 authToken이 없습니다');
      }
      
      const { authToken } = data;
      console.log('[AuthScreen] 🔑 서비스 JWT(authToken):', authToken.substring(0, 10) + '...');

      // 3) 로컬 스토리지 대신 Capacitor Preferences 에 저장
      await Preferences.set({ key: 'AUTH_TOKEN', value: authToken });
      console.log('[AuthScreen] 인증 토큰 저장 완료');

      // [추가] FCM 토큰 등록 - 이 부분이 중요합니다!
      try {
        console.log('[AuthScreen] FCM 토큰 등록 시도 시작');
        await registerFcmToken(authToken);
        console.log('[AuthScreen] FCM 토큰 등록 요청 완료');
        // FCM 토큰 등록 상태 저장
        await Preferences.set({ key: 'fcm_token_registered', value: 'true' });
      } catch (fcmError: any) {
        console.error('[AuthScreen] FCM 토큰 등록 중 오류:', fcmError.message || fcmError);
        // FCM 오류가 발생해도 로그인 진행
      }

      // 4️⃣ 대시보드 페이지로 이동
      console.log('[AuthScreen] 대시보드로 이동');
      router.replace('/dashboard');
    } catch (e: any) {
      console.error('[AuthScreen] 로그인 처리 중 에러:', e.message || e);
      // 오류 유형에 따른 더 자세한 로그
      if (e instanceof Error) {
        console.error('[AuthScreen] 에러 이름:', e.name);
        console.error('[AuthScreen] 에러 메시지:', e.message);
        console.error('[AuthScreen] 에러 스택:', e.stack);
      }
      
      // 사용자에게 오류 알림
      alert(`로그인 처리 중 오류가 발생했습니다: ${e.message || '알 수 없는 오류'}`);
    } finally {
      setIsLoading(false);
    }
  };

  const handleLogin = async () => {
    // 비로그인 상태로 대시보드 접근을 위해 임시 토큰 저장
    const token = process.env.NEXT_PUBLIC_JWT // 환경변수는 NEXT_PUBLIC_ 붙여야 클라이언트 접근 가능
    if (token) {
      console.log('[AuthScreen] 임시 로그인 시작, 토큰:', token.substring(0, 10) + '...');
      
      try {
        // 토큰 저장
        await Preferences.set({ key: 'AUTH_TOKEN', value: token });
        console.log('[AuthScreen] 임시 로그인 토큰 저장 완료');
        
        // 네이티브 브릿지 확인 (디버깅 목적)
        if (Capacitor.isNativePlatform()) {
          console.log('[AuthScreen] 네이티브 플랫폼에서 실행 중');
          console.log('[AuthScreen] FCM 브릿지 확인:', 
            (window as any).androidFcmBridge ? '✅ 존재함' : '❌ 존재하지 않음');
          console.log('[AuthScreen] MainActivity 브릿지 확인:', 
            (window as any).MainActivity ? '✅ 존재함' : '❌ 존재하지 않음');
        }
        
        // FCM 토큰 등록
        try {
          console.log('[AuthScreen] 임시 로그인 - FCM 토큰 등록 시도');
          await registerFcmToken(token);
          console.log('[AuthScreen] 임시 로그인 - FCM 토큰 등록 완료');
          // FCM 토큰 등록 상태 저장
          await Preferences.set({ key: 'fcm_token_registered', value: 'true' });
        } catch (fcmError: any) {
          console.error('[AuthScreen] 임시 로그인 - FCM 토큰 등록 오류:', fcmError.message || fcmError);
          // FCM 등록 실패해도 계속 진행
        }
        
        // 대시보드로 이동
        console.log('[AuthScreen] 임시 로그인 - 대시보드로 이동');
        router.replace('/dashboard');
      } catch (error: any) {
        console.error('[AuthScreen] 임시 로그인 에러:', error.message || error);
        alert('임시 로그인 중 오류가 발생했습니다: ' + (error.message || '알 수 없는 오류'));
      }
    } else {
      console.error("JWT 토큰이 존재하지 않습니다.");
      alert("JWT 토큰이 존재하지 않습니다. 환경 변수를 확인해주세요.");
    }
  };

  return (
    <div className="flex flex-col min-h-screen bg-black text-white">
      {/* 슬라이드 섹션 */}
      <div
        className="flex-1 flex flex-col items-center justify-center p-6 relative"
        ref={sliderRef}
        onTouchStart={handleTouchStart}
        onTouchMove={handleTouchMove}
        onTouchEnd={handleTouchEnd}
      >
        <div className="w-full max-w-md">
          {/* 이미지 그리드 */}
          <div className="mb-8">
            {slides[currentSlide].images.map((image, index) => (
              <motion.div
                key={index}
                initial={{ opacity: 0, y: 20 }}
                animate={{ opacity: 1, y: 0 }}
                transition={{ delay: index * 0.1 }}
                className="relative border border-gray-700 rounded-lg overflow-hidden"
              >
                <img
                  src={image || "/placeholder.svg"}
                  alt={`Feature illustration ${index + 1}`}
                  className="w-full h-auto"
                />
              </motion.div>
            ))}
          </div>

          {/* 텍스트 콘텐츠 */}
          <motion.div
            key={currentSlide}
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            className="mb-8"
          >
            <h2 className="text-2xl font-bold mb-2">{slides[currentSlide].title}</h2>
            <p className="text-gray-400">{slides[currentSlide].description}</p>
          </motion.div>

          {/* 슬라이드 인디케이터 및 네비게이션 */}
          <div className="flex items-center justify-center">
            <button onClick={handlePrevSlide} className="p-2 text-gray-400 hover:text-white" aria-label="이전 슬라이드">
              <ChevronLeft size={20} />
            </button>

            <div className="flex space-x-2 mx-4">
              {slides.map((_, index) => (
                <button
                  key={index}
                  onClick={() => setCurrentSlide(index)}
                  className={`w-2 h-2 rounded-full transition-colors ${
                    currentSlide === index ? "bg-red-600" : "bg-gray-600"
                  }`}
                  aria-label={`슬라이드 ${index + 1}`}
                />
              ))}
            </div>

            <button onClick={handleNextSlide} className="p-2 text-gray-400 hover:text-white" aria-label="다음 슬라이드">
              <ChevronRight size={20} />
            </button>
          </div>
        </div>
      </div>

      {/* 로그인/회원가입 버튼 */}
      <div className="p-6 space-y-4">
        <button
          onClick={handleKakaoLogin}
          disabled={isLoading}
          className="w-full py-4 flex items-center justify-center space-x-2 text-black font-medium rounded-md bg-[#FEE500] hover:bg-[#FDD835] transition-colors"
        >
          {isLoading ? (
            <div className="w-5 h-5 border-2 border-black border-t-transparent rounded-full animate-spin" />
          ) : (
            <>
              <Image src="/kakao-logo.png" alt="Kakao" width={24} height={24} className="mr-2" />
              카카오톡으로 로그인하기
            </>
          )}
        </button>


        <div className="relative flex items-center justify-center">
          <div className="absolute inset-0 flex items-center">
            <div className="w-full border-t border-gray-700"></div>
          </div>
          <div className="relative px-4 text-sm text-gray-400 bg-black">또는</div>
        </div>

        <button
          onClick={handleSkip}
          className="w-full py-4 text-gray-300 font-medium rounded-md border border-gray-700 hover:bg-gray-900 transition-colors"
        >
          다음에 하기
        </button>

        <p className="text-xs text-center text-gray-500 mt-6">
          로그인하면 블랙박스 리뷰의 <span className="text-gray-400">서비스 이용약관</span>과{" "}
          <span className="text-gray-400">개인정보 처리방침</span>에 동의하게 됩니다.
        </p>
      </div>
    </div>
  )
}
