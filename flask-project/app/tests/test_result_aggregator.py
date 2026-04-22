from app.core.config import Settings
from app.schemas import ActionTimeline, VideoDetectionTarget
from app.services.result_aggregator import ResultAggregator


def build_target(score: float, detection_count: int) -> VideoDetectionTarget:
    return VideoDetectionTarget(
        label="PILL",
        score=score,
        bbox=(0.1, 0.1, 0.4, 0.4),
        firstDetectedFrame=0,
        lastDetectedFrame=max(detection_count - 1, 0),
        detectionCount=detection_count,
    )


def build_segment(start: int, end: int, confidence: float) -> ActionTimeline:
    return ActionTimeline(startFrame=start, endFrame=end, confidence=confidence)


def test_assessment_marks_clear_intake_when_target_and_action_are_both_strong():
    aggregator = ResultAggregator(Settings())
    assessment = aggregator.calculate_overall_status(
        [build_target(0.9, 60)],
        [build_segment(10, 80, 0.92)],
        total_frames=120,
    )

    assert assessment.status == "confirmed"
    assert assessment.reason_code == "clear_intake"
    assert assessment.risk_tag == "clear_intake"
    assert assessment.final_confidence >= 0.75


def test_assessment_marks_possible_fake_intake_when_target_strong_but_action_missing():
    aggregator = ResultAggregator(Settings())
    assessment = aggregator.calculate_overall_status(
        [build_target(0.88, 58)],
        [],
        total_frames=120,
    )

    assert assessment.status == "suspected"
    assert assessment.reason_code == "possible_fake_intake"
    assert assessment.risk_tag == "possible_fake_intake"


def test_assessment_marks_action_only_when_no_target_is_detected():
    aggregator = ResultAggregator(Settings())
    assessment = aggregator.calculate_overall_status(
        [],
        [build_segment(20, 55, 0.81)],
        total_frames=120,
    )

    assert assessment.status == "suspected"
    assert assessment.reason_code == "action_only"
    assert assessment.risk_tag == "insufficient_evidence"


def test_assessment_marks_no_medication_detected_when_no_evidence_exists():
    aggregator = ResultAggregator(Settings())
    assessment = aggregator.calculate_overall_status([], [], total_frames=120)

    assert assessment.status == "abnormal"
    assert assessment.reason_code == "no_medication_detected"
    assert assessment.risk_tag == "no_target"
