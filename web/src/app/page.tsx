import { Button } from "@/components/ui/button";
import { Card, CardHeader, CardTitle, CardContent } from "@/components/ui/card";
import { Input } from "@/components/ui/input";

export default function Home() {
  return (
    <main className="flex min-h-screen items-center justify-center p-8">
      <Card className="w-96">
        <CardHeader>
          <CardTitle>Rikkei Jobs</CardTitle>
        </CardHeader>
        <CardContent className="space-y-3">
          <Input placeholder="Tìm công việc..." />
          <Button className="w-full">Tìm kiếm</Button>
        </CardContent>
      </Card>
    </main>
  );
}