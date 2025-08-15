-- ===== USER =====
INSERT INTO `user` (`id`,`name`,`password`,`prescription_history`) VALUES
(1,'JohnSmith','password123',''),
(2,'EmilyClark','password123',''),
(3,'MichaelBrown','password123',''),
(4,'SarahWilson','password123',''),
(5,'DavidJohnson','password123',''),
(6,'LiWei','password123',''),
(7,'WangFang','password123',''),
(8,'ChenHao','password123',''),
(9,'LiuMei','password123',''),
(10,'ZhangWei','password123','');

-- ===== PRESCRIPTION =====
INSERT INTO prescription (id, content, korean_content, user_id, images, created_at, title) VALUES
(1,"[Pre-Visit Patient Information]

Patient Name: Hong Gil-dong (June 12, 1985, Female)

Contact: +82-10-1234-5678

1. Purpose of Visit: Symptom consultation
2. Main Symptoms: Sore throat, cough, mild fever (onset: 2025-08-10, worsening)
3. Past Medical History: Hypertension (since 2020), Allergic rhinitis (since 2015)
4. Surgical History: Appendectomy (2005)
5. Allergies: Penicillin antibiotics
6. Current Medications: Antihypertensive (once daily in the morning), Vitamin D (three times a week)
7. Lifestyle: Non-smoker, Rare alcohol use, Exercises twice a week (walking, yoga)
8. Infectious Disease Risk: No overseas travel in the past 14 days, No contact with infectious disease patients
9. Other Notes: Not pregnant, Recently experiencing increased stress and lack of sleep","[사전 문진 정보]

환자 이름: 홍길동 (1985-06-12, 여성)

연락처: 010-1234-5678

1. 방문 목적: 증상 진료
2. 주요 증상: 목 통증, 기침, 미열 (2025-08-10부터 시작, 악화 중)
3. 과거 병력: 고혈압(2020~), 비염(2015~)
4. 수술 이력: 맹장 수술(2005)
5. 알레르기: 펜실린 계열 항생제
6. 복용 약물: 고혈압 약(매일 아침 1회), 비타민 D(주 3회)
7. 생활 습관: 비흡연, 음주 거의 안 함, 주 2회 운동(걷기, 요가)
8. 감염 관련: 최근 14일 이내 해외여행 없음, 감염병 환자 접촉 없음
9. 기타: 임신 아님, 최근 스트레스 증가로 수면 부족",1, '', '2025-08-12', '목 통증·기침·미열 증상 내원'),
(2,NULL,NULL,2, '', '2025-08-10', '내용 없음'),
(3,NULL,NULL,3, '', '2025-08-09', '내용 없음'),
(4,NULL,NULL,4, '', '2025-08-08', '내용 없음'),
(5,NULL,NULL,5, '', '2025-08-07', '내용 없음'),
(6,NULL,NULL,6, '', '2025-08-06', '내용 없음'),
(7,NULL,NULL,7, '', '2025-08-05', '내용 없음'),
(8,NULL,NULL,8, '', '2025-08-04', '내용 없음'),
(9,NULL,NULL,9, '', '2025-08-03', '내용 없음'),
(10,NULL,NULL,10, '', '2025-08-02', '내용 없음');



-- ===== PRECHECK =====
INSERT INTO `precheck`
(`id`,`title`,`content`,`created_at`,`name`,`age`,`nationality`,`gender`,`description`,`user_id`) VALUES
(1,NULL,NULL,'2025-08-13','John Smith',35,'USA','M','I have a sore throat, mild fever, and headache for 2 days.',1),
(2,NULL,NULL,'2025-08-13','Emily Clark',28,'UK','F','I\'ve been coughing with green phlegm and chest tightness.',2),
(3,NULL,NULL,'2025-08-13','Michael Brown',42,'Australia','M','Fever, body aches, and chills started last night.',3),
(4,NULL,NULL,'2025-08-13','Sarah Wilson',30,'Canada','F','My nose is congested, and I can\'t stop sneezing.',4),
(5,NULL,NULL,'2025-08-13','David Johnson',26,'NewZealand','M','I have itchy eyes and runny nose in the morning.',5),
(6,NULL,NULL,'2025-08-13','Li Wei',40,'China','M','Persistent cough with occasional shortness of breath and mild fever for 3 days.',6),
(7,NULL,NULL,'2025-08-13','Wang Fang',33,'Taiwan','F','Sharp abdominal pain and nausea that started this morning.',7),
(8,NULL,NULL,'2025-08-13','Chen Hao',29,'HongKong','M','Mild dizziness and fatigue, especially in the afternoons.',8),
(9,NULL,NULL,'2025-08-13','Liu Mei',31,'China','F','Skin rash with itching on arms and neck for the past week.',9),
(10,NULL,NULL,'2025-08-13','Zhang Wei',27,'Taiwan','M','Mild back pain after lifting heavy objects two days ago.',10);
