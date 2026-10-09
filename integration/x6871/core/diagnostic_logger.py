"""
diagnostic_logger.py

Structured logging and evidentiary audit tracking with mandatory epistemological tagging:
- VERIFIED: Directly proven by primary binary inspection or test execution.
- STRONGLY_INDICATED: Supported by strong circumstantial evidence (e.g. build dates, standard specs).
- UNVERIFIED: Plausible but requires live hardware telemetry or missing files.
- DISPROVED: Refuted by empirical counter-evidence.
"""

from enum import Enum
from dataclasses import dataclass, field
from typing import List, Dict, Any
import json
import time

class FindingClassification(str, Enum):
    VERIFIED = "VERIFIED"
    STRONGLY_INDICATED = "STRONGLY_INDICATED"
    UNVERIFIED = "UNVERIFIED"
    DISPROVED = "DISPROVED"

@dataclass
class DiagnosticFinding:
    topic: str
    classification: FindingClassification
    summary: str
    evidence_source: str
    details: str = ""
    timestamp: float = field(default_factory=time.time)

    def to_dict(self) -> Dict[str, Any]:
        return {
            "topic": self.topic,
            "classification": self.classification.value,
            "summary": self.summary,
            "evidence_source": self.evidence_source,
            "details": self.details,
            "timestamp": self.timestamp
        }

class DiagnosticLogger:
    def __init__(self, context_name: str = "SpeedLock"):
        self.context_name = context_name
        self.findings: List[DiagnosticFinding] = []

    def log_finding(self, topic: str, classification: FindingClassification,
                    summary: str, evidence_source: str, details: str = "") -> DiagnosticFinding:
        finding = DiagnosticFinding(
            topic=topic,
            classification=classification,
            summary=summary,
            evidence_source=evidence_source,
            details=details
        )
        self.findings.append(finding)
        return finding

    def get_by_classification(self, classification: FindingClassification) -> List[DiagnosticFinding]:
        return [f for f in self.findings if f.classification == classification]

    def to_markdown(self) -> str:
        lines = [
            f"# Diagnostic Findings Report — {self.context_name}",
            "",
            "| Topic | Classification | Summary | Evidence Source |",
            "|---|---|---|---|"
        ]
        for f in self.findings:
            badge = f"**{f.classification.value}**"
            lines.append(f"| {f.topic} | {badge} | {f.summary} | `{f.evidence_source}` |")
        lines.append("")
        return "\n".join(lines)

    def to_json(self, indent: int = 2) -> str:
        return json.dumps([f.to_dict() for f in self.findings], indent=indent)
