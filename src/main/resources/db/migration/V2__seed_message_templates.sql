-- ============================================================================
-- Initial message templates (event_type x channel)
-- ============================================================================

INSERT INTO message_templates (event_type, channel, version, subject, body, enabled, created_at, updated_at)
VALUES
('NOTI_COMMENT', 'MAIL', 1,
 '[Locker Room] 회원님의 게시글에 새 댓글이 달렸어요',
 '안녕하세요. {{actorNickname}}님이 회원님의 게시글에 댓글을 남겼습니다.\n\n게시글 보러가기: https://lockerroom.local/posts/{{postId}}',
 TRUE, NOW(6), NOW(6)),

('NOTI_REPLY', 'MAIL', 1,
 '[Locker Room] 회원님의 댓글에 답글이 달렸어요',
 '안녕하세요. {{actorNickname}}님이 회원님의 댓글에 답글을 남겼습니다.\n\n게시글 보러가기: https://lockerroom.local/posts/{{postId}}',
 TRUE, NOW(6), NOW(6)),

('NOTI_INQUIRY_REPLIED', 'MAIL', 1,
 '[Locker Room] 1:1 문의 답변이 도착했어요',
 '회원님의 문의에 답변이 등록되었습니다.\n\n문의 내역 보기: https://lockerroom.local/inquiries/{{inquiryId}}',
 TRUE, NOW(6), NOW(6)),

('NOTI_REPORT_PROCESSED', 'MAIL', 1,
 '[Locker Room] 신고 처리 결과 안내',
 '회원님이 접수하신 신고가 처리되었습니다. (결과: {{decision}})\n\n자세한 내용은 마이페이지에서 확인하실 수 있습니다.',
 TRUE, NOW(6), NOW(6)),

('NOTI_COMMENT', 'SMS', 1,
 NULL,
 '[Locker Room] {{actorNickname}}님이 회원님의 게시글에 댓글을 남겼습니다.',
 TRUE, NOW(6), NOW(6)),

('NOTI_REPLY', 'SMS', 1,
 NULL,
 '[Locker Room] {{actorNickname}}님이 회원님의 댓글에 답글을 남겼습니다.',
 TRUE, NOW(6), NOW(6)),

('NOTI_INQUIRY_REPLIED', 'SMS', 1,
 NULL,
 '[Locker Room] 1:1 문의 답변이 도착했습니다. 마이페이지에서 확인해주세요.',
 TRUE, NOW(6), NOW(6)),

('NOTI_REPORT_PROCESSED', 'SMS', 1,
 NULL,
 '[Locker Room] 신고 처리가 완료되었습니다. (결과: {{decision}})',
 TRUE, NOW(6), NOW(6));
