import { test, expect } from "@playwright/test";

const candidate = {
  fullName: "Jahnavi Test",
  email: "jahnavi@example.com",
};

async function installApiMocks(page, options = {}) {
  let jobs = options.jobs ? [...options.jobs] : [];
  let resumes = options.resumes ? [...options.resumes] : [];
  let applications = [];
  let nextJobId = 100;
  let nextResumeId = 200;
  let nextApplicationId = 300;
  const seenAuthorizationHeaders = [];

  const stats = () => ({
    totalApplied: applications.filter((item) =>
      ["SUBMITTED", "UNDER_REVIEW", "INTERVIEW", "REJECTED", "OFFER"].includes(item.status)
    ).length,
    underReview: applications.filter((item) => item.status === "UNDER_REVIEW").length,
    interviews: applications.filter((item) => item.status === "INTERVIEW").length,
    offers: applications.filter((item) => item.status === "OFFER").length,
    rejected: applications.filter((item) => item.status === "REJECTED").length,
    saved: applications.filter((item) =>
      ["SAVED", "PREPARING", "PENDING_APPROVAL"].includes(item.status)
    ).length,
  });

  await page.route("**/api/v1/**", async (route) => {
    const request = route.request();
    const method = request.method();
    const url = new URL(request.url());
    const path = url.pathname.replace(/^\/api\/v1/, "");
    const auth = request.headers()["authorization"];
    if (auth) seenAuthorizationHeaders.push(auth);

    const json = (body, status = 200) =>
      route.fulfill({
        status,
        contentType: "application/json",
        body: JSON.stringify(body),
      });

    if (method === "POST" && path === "/auth/login") {
      const body = JSON.parse(request.postData() || "{}");
      if (body.email !== candidate.email || body.password !== "StrongPassword123!") {
        return json({ message: "Invalid email or password" }, 401);
      }
      return json({
        token: "playwright-test-token",
        user: { id: 1, ...candidate },
      });
    }

    if (method === "GET" && path === "/applications/stats") return json(stats());
    if (method === "GET" && path === "/applications") return json(applications);
    if (method === "GET" && path === "/jobs") return json(jobs);
    if (method === "GET" && path === "/resumes") return json(resumes);

    if (method === "POST" && path === "/jobs") {
      const body = JSON.parse(request.postData() || "{}");
      const job = {
        id: nextJobId++,
        companyName: body.companyName,
        jobTitle: body.jobTitle,
        jobDescription: body.jobDescription || "",
        location: body.location || "",
        jobType: body.jobType || "FULL_TIME",
        experienceLevel: body.experienceLevel || "FRESHER",
        graduationYear: body.graduationYear || 2027,
        applicationUrl: body.applicationUrl || "",
        source: "USER_ADDED",
      };
      jobs = [job, ...jobs];
      return json(job, 201);
    }

    if (method === "POST" && path === "/applications") {
      const body = JSON.parse(request.postData() || "{}");
      if (applications.some((item) => item.jobId === body.jobId)) {
        return json({ message: "You already have an application record for this job" }, 409);
      }
      const job = jobs.find((item) => item.id === body.jobId);
      if (!job) return json({ message: "Job not found" }, 404);
      const application = {
        id: nextApplicationId++,
        jobId: job.id,
        companyName: job.companyName,
        jobTitle: job.jobTitle,
        location: job.location || "",
        status: "SAVED",
        applicationMode: "MANUAL",
        notes: "",
        submittedAt: "",
        interviewDate: "",
        followUpDate: "",
        createdAt: new Date().toISOString(),
      };
      applications = [application, ...applications];
      return json(application, 201);
    }

    const statusMatch = path.match(/^\/applications\/(\d+)\/status$/);
    if (method === "PATCH" && statusMatch) {
      const application = applications.find((item) => item.id === Number(statusMatch[1]));
      if (!application) return json({ message: "Application not found" }, 404);
      const body = JSON.parse(request.postData() || "{}");
      application.status = body.status;
      if (body.status === "SUBMITTED" && !application.submittedAt) {
        application.submittedAt = new Date().toISOString();
      }
      return json(application);
    }

    if (method === "POST" && path === "/resumes/upload") {
      const resume = {
        id: nextResumeId++,
        title: "Uploaded Resume",
        fileName: "jahnavi-resume.docx",
        characterCount: 420,
        isMaster: resumes.length === 0,
        createdAt: new Date().toISOString(),
      };
      resumes = [resume, ...resumes.map((item) => ({
        ...item,
        isMaster: resume.isMaster ? false : item.isMaster,
      }))];
      return json({ id: resume.id, title: resume.title, fileName: resume.fileName,
        isMaster: resume.isMaster, characterCount: resume.characterCount }, 201);
    }

    if (method === "POST" && path === "/ats/analyze") {
      return json({
        overallScore: 82,
        keywordCoverage: 78,
        structureScore: 90,
        matchedKeywords: ["Java", "Spring Boot", "MySQL"],
        missingKeywords: ["Docker"],
        recommendations: ["Add measurable project results."],
        disclaimer: "This is an estimate, not an employer ATS score.",
      });
    }

    if (method === "POST" && path === "/ai/generate-cover-letter") {
      return json({
        coverLetter: "Dear Hiring Team,\n\nI am interested in the Java Backend Intern role at Northstar Labs.\n\nRegards,\nJahnavi",
        requiresReview: true,
        note: "Review every statement before sending.",
      });
    }

    if (method === "POST" && path === "/ai/tailor-resume") {
      return json({ tailoredText: "Jahnavi Test\nJava Backend Projects", requiresReview: true });
    }

    if (method === "PUT" && /^\/resumes\/\d+\/master$/.test(path)) {
      const id = Number(path.split("/")[2]);
      resumes = resumes.map((item) => ({ ...item, isMaster: item.id === id }));
      return json({ id, isMaster: true, title: resumes.find((item) => item.id === id)?.title });
    }

    return json({ message: `Unmocked API request: ${method} ${path}` }, 404);
  });

  return { getAuthorizationHeaders: () => seenAuthorizationHeaders };
}

async function signIn(page) {
  await page.goto("/");
  await page.getByLabel("Email address").fill(candidate.email);
  await page.getByLabel("Password").fill("StrongPassword123!");
  await page.getByRole("button", { name: "Sign in" }).click();
  await expect(page.getByRole("heading", { name: /good things start/i })).toBeVisible();
}

test("candidate can add a job, save it, and update its application status", async ({ page }) => {
  const mocks = await installApiMocks(page);
  await signIn(page);

  await page.getByRole("button", { name: "Find Jobs" }).click();
  await page.getByPlaceholder("Company name").fill("Northstar Labs");
  await page.getByPlaceholder("Job title", { exact: true }).fill("Java Backend Intern");
  await page.getByPlaceholder("Location").fill("Remote");
  await page.getByPlaceholder("Application URL").fill("https://example.com/jobs/java");
  await page.getByPlaceholder("Paste the job description…").fill(
    "Build Java REST APIs using Spring Boot and MySQL."
  );
  await page.getByRole("button", { name: "Add to my jobs" }).click();

  const jobCard = page.locator(".job-row").filter({ hasText: "Java Backend Intern" });
  await expect(jobCard).toBeVisible();
  await jobCard.getByRole("button", { name: "Save job" }).click();
  await expect(page.getByText("Job saved to your tracker.")).toBeVisible();

  await page.getByRole("button", { name: "Applications" }).click();
  const applicationRow = page.getByRole("row").filter({ hasText: "Java Backend Intern" });
  await expect(applicationRow).toBeVisible();
  await applicationRow.locator("select").selectOption("SUBMITTED");
  await expect(applicationRow.locator("select")).toHaveValue("SUBMITTED");

  expect(mocks.getAuthorizationHeaders().length).toBeGreaterThan(0);
  expect(mocks.getAuthorizationHeaders().every((value) => value === "Bearer playwright-test-token")).toBeTruthy();
});

test("candidate can upload a resume and generate an editable cover letter", async ({ page }) => {
  await installApiMocks(page, {
    jobs: [{
      id: 11,
      companyName: "Northstar Labs",
      jobTitle: "Java Backend Intern",
      jobDescription: "Java, Spring Boot, REST APIs and MySQL",
      location: "Remote",
      jobType: "INTERNSHIP",
      experienceLevel: "FRESHER",
      graduationYear: 2027,
      applicationUrl: "https://example.com/jobs/java",
      source: "USER_ADDED",
    }],
  });
  await signIn(page);

  await page.getByRole("button", { name: "My Resume" }).click();
  await page.getByPlaceholder("Master resume").fill("Jahnavi Backend Resume");
  await page.getByLabel("Choose resume").setInputFiles({
    name: "jahnavi-resume.docx",
    mimeType: "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
    buffer: Buffer.from("mock DOCX payload"),
  });
  await page.getByRole("button", { name: "Upload and parse" }).click();
  await expect(page.getByText("Uploaded Resume")).toBeVisible();

  await page.getByRole("button", { name: "Cover Letter" }).click();
  await page.locator(".tool-panel select").nth(0).selectOption({ label: "Uploaded Resume" });
  await page.locator(".tool-panel select").nth(1).selectOption({ label: "Java Backend Intern — Northstar Labs" });
  await page.getByRole("button", { name: "Generate cover letter" }).click();

  const editor = page.getByLabel("Edit cover letter draft");
  await expect(editor).toHaveValue(/Northstar Labs/);
  await editor.fill("My reviewed cover letter draft.");
  await expect(editor).toHaveValue("My reviewed cover letter draft.");

  const downloadPromise = page.waitForEvent("download");
  await page.getByRole("button", { name: "Download TXT" }).click();
  const download = await downloadPromise;
  expect(download.suggestedFilename()).toBe("jobapply-cover-letter.txt");
});

test("invalid login keeps the candidate on the sign-in screen with a useful error", async ({ page }) => {
  await installApiMocks(page);
  await page.goto("/");
  await page.getByLabel("Email address").fill("wrong@example.com");
  await page.getByLabel("Password").fill("wrong-password");
  await page.getByRole("button", { name: "Sign in" }).click();

  await expect(page.getByText("Invalid email or password")).toBeVisible();
  await expect(page.getByRole("heading", { name: "Welcome back." })).toBeVisible();
});
