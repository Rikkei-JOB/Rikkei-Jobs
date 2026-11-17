import { Card, CardContent } from "@/app/components/ui/card";
import { companyService } from "@/services/company.service";
import type { Company } from "@/interface/company";

/** Chỉ cho phép liên kết http/https để không render URL kiểu javascript:. */
function isHttpUrl(value: string | null): value is string {
  return value !== null && /^https?:\/\//i.test(value);
}

/**
 * Trang công ty như ứng viên thấy. Không hiện thông tin nội bộ (mã số thuế, trạng thái, ngày tạo).
 * Dùng chung cho trang xem trước của HR và trang ứng viên sau này.
 */
export function CompanyPublicView({ company }: { company: Company }) {
  const logo = companyService.logoSrc(company.logoUrl);
  const meta = [company.industry, company.size ? `${company.size} nhân sự` : null, company.city]
    .filter(Boolean)
    .join(" · ");

  return (
    <div className="space-y-4">
      <Card>
        <CardContent className="space-y-4">
          <div className="flex items-center gap-4">
            <div className="flex h-20 w-20 shrink-0 items-center justify-center overflow-hidden rounded-lg border bg-muted/40">
              {logo ? (
                // eslint-disable-next-line @next/next/no-img-element
                <img src={logo} alt={`Logo ${company.name}`} className="h-full w-full object-contain" />
              ) : (
                <span className="text-xs text-muted-foreground">Chưa có logo</span>
              )}
            </div>
            <div className="min-w-0 space-y-1">
              <h2 className="font-heading text-lg font-semibold break-words">{company.name}</h2>
              {meta ? <p className="text-xs text-muted-foreground">{meta}</p> : null}
            </div>
          </div>

          {isHttpUrl(company.website) || company.address ? (
            <dl className="space-y-1 text-xs/relaxed">
              {isHttpUrl(company.website) ? (
                <div>
                  <dt className="inline text-muted-foreground">Website: </dt>
                  <dd className="inline break-all">
                    <a
                      href={company.website}
                      target="_blank"
                      rel="noopener noreferrer"
                      className="text-primary underline underline-offset-2"
                    >
                      {company.website}
                    </a>
                  </dd>
                </div>
              ) : null}
              {company.address ? (
                <div>
                  <dt className="inline text-muted-foreground">Địa chỉ: </dt>
                  <dd className="inline">{company.address}</dd>
                </div>
              ) : null}
            </dl>
          ) : null}
        </CardContent>
      </Card>

      <Card>
        <CardContent className="space-y-2">
          <h3 className="font-heading text-sm font-semibold">Giới thiệu</h3>
          {company.description ? (
            <p className="text-xs/relaxed break-words whitespace-pre-line">{company.description}</p>
          ) : (
            <p className="text-xs text-muted-foreground">Công ty chưa có phần giới thiệu.</p>
          )}
        </CardContent>
      </Card>

      <Card>
        <CardContent className="space-y-2">
          <h3 className="font-heading text-sm font-semibold">Việc làm đang tuyển (0)</h3>
          <p className="text-xs text-muted-foreground">Hiện chưa có vị trí nào đang tuyển.</p>
        </CardContent>
      </Card>
    </div>
  );
}
