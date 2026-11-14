"use client";

import * as React from "react";
import { SkillProfile } from "@/app/components/skills/skill-profile";
import { SelfSkills } from "@/app/components/skills/self-skills";

export function CandidateSkills() {
  const [revision, setRevision] = React.useState(0);
  return <div className="space-y-6">
    <SelfSkills onSaved={() => setRevision(value => value + 1)} />
    <section aria-label="Skill Profile"><SkillProfile key={revision} /></section>
  </div>;
}
