import { api } from "@/services/api";
import type { InterviewCalendarItem, InterviewInvitation, InterviewPage, InterviewProposal, InterviewSchedule, SlotInput } from "@/interface/interview";
const root = "/recruitment";
export interface CalendarParams { from: string; to: string; interviewerEmail?: string; status?: string }
async function allPages<T>(load: (page: number) => Promise<InterviewPage<T>>) {
  const items: T[] = [];
  for (let page = 1; ; page++) {
    const result = await load(page); items.push(...result.items);
    if (items.length >= result.total) return items;
    if (!result.items.length) throw new Error("Danh sách lịch thay đổi. Vui lòng tải lại.");
  }
}
export const interviewService = {
  async propose(roundId: string, slots: SlotInput[]) { return (await api.post<InterviewProposal>(`${root}/rounds/${encodeURIComponent(roundId)}/slots`, slots)).data; },
  async reschedule(roundId: string, slots: SlotInput[]) { return (await api.post<InterviewProposal>(`${root}/rounds/${encodeURIComponent(roundId)}/reschedule`, slots)).data; },
  async pick(roundId: string, slotId: string) { return (await api.post<InterviewSchedule>(`${root}/me/interviews/${encodeURIComponent(roundId)}/pick`, { slotId })).data; },
  async listMine(params: { page?: number; size?: number }, signal?: AbortSignal) { return (await api.get<InterviewPage<InterviewInvitation>>(`${root}/me/interviews`, { params, signal })).data; },
  async listCompany(params: CalendarParams & { page?: number; size?: number }, signal?: AbortSignal) { return (await api.get<InterviewPage<InterviewCalendarItem>>(`${root}/companies/me/interviews`, { params, signal })).data; },
  mineAll(signal?: AbortSignal) { return allPages(page => this.listMine({ page, size: 100 }, signal)); },
  companyAll(params: CalendarParams, signal?: AbortSignal) { return allPages(page => this.listCompany({ ...params, page, size: 100 }, signal)); },
};
