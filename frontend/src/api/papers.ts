import { newsRequestData } from '@/api/newsAdminRequest'
import { get } from '@/axios/request'

export type PaperSource = 'CNKI' | 'ARXIV' | 'CROSSREF' | 'EUROPE_PMC'

export interface PaperQuery {
  keyword: string
  source: PaperSource
}

export interface PaperHit {
  title: string
  authors: string | null
  publication: string | null
  publicationDate: string | null
  summary: string | null
  doi: string | null
  url: string
}

interface PaperSearchPage {
  items: PaperHit[]
  nextCursor: string | null
}

export const searchPapersApi = (query: PaperQuery, cursor?: string) =>
  newsRequestData(get<PaperSearchPage>('/ai/api/web/v1/papers/search', { ...query, cursor }), false)
