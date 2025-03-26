export interface EmailInfo {
    id: string;
    emailName: string; // Subject Name
    contentTemplateId: string; // Reference to TemplateInfo's objectId
    createTime: Date | null;
    modifiedTime: Date | null;
    sentTime: Date | null;
    createdBy: string;
    to: string[];
    cc: string[];
    status?: string; // DRAFT, SCHEDULED, SENT, FAILED
    errorMessage?: string;
    attachments?: string[];
  }
  